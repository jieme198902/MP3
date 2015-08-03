package com.zero.mp3.activites;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.zero.mp3.R;
import com.zero.mp3.Utils.L;
import com.zero.mp3.Utils.T;
import com.zero.mp3.adapter.MusicListAdapter;
import com.zero.mp3.app.AppContext;
import com.zero.mp3.beans.Music;
import com.zero.mp3.service.PlayService;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener{
    private final static String TAG = "MainActivity";

    private List<Music> mMusics;
    private MusicListAdapter mAdapter;
    private boolean isPlaying;


    private String url="";

    @Bind(R.id.update_music_pb)
    ProgressBar mUpdatePBar;

    @Bind(R.id.music_list_lv)
    ListView mMusicListView;

    //下一曲
    @Bind(R.id.music_function_next_iv)
    ImageView mNextIv;

    //播放或暂停
    @Bind(R.id.music_function_play_iv)
    ImageView mPlayIv;

    //上一曲
    @Bind(R.id.music_function_previous_iv)
    ImageView mPreviousIv;

    //底部显示歌曲名
    @Bind(R.id.music_title_tv)
    TextView mBottomTitle;

    //底部显示歌手名
    @Bind(R.id.singer_name_tv)
    TextView mBottomName;

    @Bind(R.id.add_fab)
    FloatingActionButton add_fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        L.d(TAG);
        initData();
        mMusicListView.setAdapter(mAdapter);
        mMusicListView.setOnItemClickListener(this);
        new getMusicTask().execute();

    }

    public void initData(){
        isPlaying = false;
        add_fab.attachToListView(mMusicListView);
        mMusics = new ArrayList<>();
        mAdapter = new MusicListAdapter(this,mMusics);
    }

    /**
     * 更新音乐列表
     */
    public void refreshData(){
        mUpdatePBar.setVisibility(View.VISIBLE);
        new getMusicTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    /**
     * 从内存卡中读取音乐列表
     * @return
     */
    public List<Music> getData(){
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        List<Music> musics = new ArrayList<>();

        for (int i = 0; i < cursor.getCount(); i++)
        {
            Music music = new Music();
            cursor.moveToNext();
            long id = cursor.getLong(cursor
                    .getColumnIndex(MediaStore.Audio.Media._ID));   //音乐id
            String title = cursor.getString((cursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE)));//音乐标题
            String artist = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST));//艺术家
            long duration = cursor.getLong(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION));//时长
            long size = cursor.getLong(cursor
                    .getColumnIndex(MediaStore.Audio.Media.SIZE));  //文件大小
            String url = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA));   //文件路径
            int isMusic = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));//是否为音乐

            if (isMusic != 0 )
            {
                music.setId(id);
                music.setTitle(title);
                music.setAirtist(artist);
                music.setDuration(duration);
                music.setSize(size);
                music.setUrl(url);
                musics.add(music);
            }
        }
        return musics;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Music music = mMusics.get(position);
        url = music.getUrl();
        Intent intent = new Intent(this,PlayService.class);
        intent.putExtra("url",url);
        L.d(TAG, music.getUrl());
        intent.putExtra("MSG", AppContext.MUSIC_PLAY);
        L.d(TAG, AppContext.MUSIC_PLAY + "");
        startService(intent);

        setBottomDisplay(music.getTitle(), music.getAirtist());
        mPlayIv.setImageResource(R.drawable.ic_action_playback_play);
        mBottomName.setFocusable(true);
        mBottomName.setFocusableInTouchMode(true);
        isPlaying = true;
    }

    /**
     * 从数据库读取音乐的任务
     */
    private class  getMusicTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            L.d(TAG,"doInBackground");
            mMusics = getData();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            L.d(TAG, "onPostExecute");
            mAdapter.setmData(mMusics);
            mAdapter.notifyDataSetChanged();
            mUpdatePBar.setVisibility(View.GONE);
        }
    }

    public void setBottomDisplay(String title,String artist){
        mBottomTitle.setText(title);
        mBottomName.setText(artist);
    }

    /**
     * 绑定播放键事件
     */
    @OnClick(R.id.music_function_play_iv)
    public void playMusic(){
        if (isPlaying)
        {
            Intent intent = new Intent(this,PlayService.class);
            intent.putExtra("url",url);
            intent.putExtra("MSG", AppContext.MUSIC_PAUSE);
            startService(intent);
            mPlayIv.setImageResource(R.drawable.ic_action_playback_pause);
            mBottomName.setFocusable(false);
            mBottomName.setFocusableInTouchMode(false);
            isPlaying= false;
        }else{
            Intent intent = new Intent(this,PlayService.class);
            intent.putExtra("url",url);
            intent.putExtra("MSG", AppContext.MUSIC_PLAY);
            startService(intent);
            mPlayIv.setImageResource(R.drawable.ic_action_playback_play);
            mBottomName.setFocusable(true);
            mBottomName.setFocusableInTouchMode(true);
            isPlaying = true;
        }
    }

    /**
     * 绑定添加事件
     */
    @OnClick(R.id.add_fab)
    public void addFab(){
        T.showShort(this,"add fab");
    }
}