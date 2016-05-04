package com.stafiiyevskyi.mlsdev.droidfm.view.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.stafiiyevskyi.mlsdev.droidfm.JUnitTestHelper;
import com.stafiiyevskyi.mlsdev.droidfm.R;
import com.stafiiyevskyi.mlsdev.droidfm.app.event.EventCurrentTrackPause;
import com.stafiiyevskyi.mlsdev.droidfm.app.event.EventTrackStart;
import com.stafiiyevskyi.mlsdev.droidfm.app.player.MediaPlayerWrapper;
import com.stafiiyevskyi.mlsdev.droidfm.app.player.TrackPlayerEntity;
import com.stafiiyevskyi.mlsdev.droidfm.app.service.TracksPlayerService;
import com.stafiiyevskyi.mlsdev.droidfm.app.util.NetworkUtil;
import com.stafiiyevskyi.mlsdev.droidfm.app.util.PreferencesManager;
import com.stafiiyevskyi.mlsdev.droidfm.view.Navigator;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.AlbumsDetailsFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.ArtistContentDetailsFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.ArtistDetailFullFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.BaseFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.TopChartsContentFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.TrackDetailFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.chart.ArtistSearchListFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.chart.ChartTopTracksFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.signin.LoginVKDialogFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.tag.TagTopContentFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.util.AnimationUtil;
import com.stafiiyevskyi.mlsdev.droidfm.view.util.SeekBarUtils;
import com.stafiiyevskyi.mlsdev.droidfm.view.widget.MenuArrowDrawable;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.Bind;
import rx.Observable;

public class MainActivity extends BaseActivity implements Navigator, SeekBar.OnSeekBarChangeListener {
    @Bind(R.id.drawer_layout)
    DrawerLayout drNavigation;
    @Bind(R.id.nav_view)
    NavigationView nvNavigation;

    @Bind(R.id.sb_player_seekbar)
    SeekBar mSbSeekbar;
    @Bind(R.id.tv_play_track_name)
    AppCompatTextView mTvPlayTrackName;
    @Bind(R.id.iv_album_image)
    AppCompatImageView mIvAlbumsTrackImage;
    @Bind(R.id.iv_play_pause)
    AppCompatImageView mIvPlayPause;
    @Bind(R.id.tv_current_track_position)
    AppCompatTextView mTvCurrentTrackPosition;
    @Bind(R.id.tv_track_duration)
    AppCompatTextView mTvTrackTotalDuration;
//    @Bind(R.id.sm_player)
//    SlidingUpPanelLayout mSmPlayer;

    private FragmentManager mFragmentManager;


    private ActionBarDrawerToggle mDrawerToggle;
    private MenuArrowDrawable mDrawerArrowDrawable;
    private BaseFragment mFirstFragment;
    private Handler mHandler;
    private String mTrackUrl;
    private String mArtist;
    private String mTrack;
    private String mAlbumImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        mHandler = new Handler();
        setupPlayerWidget();
        mFragmentManager = getSupportFragmentManager();
        setupNavigation();
        getSupportActionBar().setSubtitle(getString(R.string.artists_section_title));
        startService(new Intent(this, TracksPlayerService.class));

        if (!JUnitTestHelper.getInstance().isJunitRunning())
            VKSdk.wakeUpSession(this, new VKCallback<VKSdk.LoginState>() {
                @Override
                public void onResult(VKSdk.LoginState res) {
                    switch (res) {
                        case LoggedOut:
                            navigateToArtistsSearchScreen();
                            navigateToLoginVKDialog();
                            return;
                        case LoggedIn:
                            navigateToArtistsSearchScreen();
                            return;
                        case Pending:
                            if (!NetworkUtil.isNetworkConnected(MainActivity.this)) {
                                Toast.makeText(MainActivity.this, "No Internet connection", Toast.LENGTH_LONG).show();
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onError(VKError error) {
                    Toast.makeText(MainActivity.this, error.errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                PreferencesManager.getInstance().setAccessToken(res.accessToken);
                PreferencesManager.getInstance().setUserId(res.userId);
                navigateToArtistsSearchScreen();
            }

            @Override
            public void onError(VKError error) {

            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            onBackPressed();
            return super.onOptionsItemSelected(item);
        }
    }

    private void setupNavigation() {
        mDrawerArrowDrawable = new MenuArrowDrawable(new ContextThemeWrapper(this, R.style.AppTheme_AppBarOverlay), getSupportActionBar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(this, drNavigation,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        nvNavigation.setNavigationItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.action_tracks_item:
                    if (!(mFirstFragment instanceof ChartTopTracksFragment)) {
                        navigateToTopTracksScreen();
                        getSupportActionBar().setSubtitle(getString(R.string.tracks_section_title));
                    }
                    drNavigation.closeDrawers();
                    return true;
                case R.id.action_artists_item:
                    if (!(mFirstFragment instanceof ArtistSearchListFragment)) {
                        navigateToArtistsSearchScreen();
                        getSupportActionBar().setSubtitle(getString(R.string.artists_section_title));
                    }

                    drNavigation.closeDrawers();
                    return true;
                case R.id.action_charts_item:
                    if (!(mFirstFragment instanceof TopChartsContentFragment)) {
                        navigateToChartsContentScreen();
                        getSupportActionBar().setSubtitle(getString(R.string.charts_section_title));
                    }
                    drNavigation.closeDrawers();
                    return true;
                default:
                    return true;
            }
        });
    }

    @Override
    public int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    public void setDrawerToggleEnabled() {
        backToMenu();
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        drNavigation.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        drNavigation.addDrawerListener(mDrawerToggle);
    }

    @Override
    public void setDrawerToggleNotEnabled() {
        menuToBack();
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        drNavigation.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void menuToBack() {
        this.mDrawerArrowDrawable.animateDrawable(true);
    }

    public void backToMenu() {
        this.mDrawerArrowDrawable.animateDrawable(false);
    }

    @Override
    public void navigateToArtistsSearchScreen() {
        mFirstFragment = ArtistSearchListFragment.newInstance();
        mFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mFirstFragment)
                .commit();
    }

    @Override
    public void navigateToChartsContentScreen() {
        mFirstFragment = TopChartsContentFragment.newInstance();
        mFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mFirstFragment)
                .commit();
    }

    @Override
    public void navigateToArtistContentDetailsScreen(String mbid, String artistName, String imageUrl, AppCompatImageView imageView) {
        BaseFragment fragment = ArtistContentDetailsFragment.newInstance(mbid, artistName, imageUrl);
        AnimationUtil.detailTransition(fragment, imageView, getString(R.string.transition_artist_image));
        mFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .addSharedElement(imageView, getString(R.string.transition_artist_image))
                .addToBackStack(ArtistContentDetailsFragment.class.getName() + mbid)
                .commit();
    }

    @Override
    public void navigateToTopTracksScreen() {
        mFirstFragment = ChartTopTracksFragment.newInstance();
        mFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mFirstFragment)
                .commit();
    }

    @Override
    public void navigateToArtistFullDetailsScreen(String mbid) {
        mFragmentManager.beginTransaction()
                .add(R.id.fragment_container, ArtistDetailFullFragment.newInstance(mbid))
                .addToBackStack(ArtistDetailFullFragment.class.getName() + mbid)
                .commit();
    }

    @Override
    public void navigateToTagTopContent(String tag) {
        getSupportActionBar().setSubtitle(tag);
        mFragmentManager.beginTransaction()
                .add(R.id.fragment_container, TagTopContentFragment.newInstance(tag))
                .addToBackStack(TagTopContentFragment.class.getName() + tag)
                .commit();
    }

    @Override
    public void navigateToAlbumDetails(String artist, String album, String mbid) {
        mFragmentManager.beginTransaction()
                .add(R.id.fragment_container, AlbumsDetailsFragment.newInstance(artist, album, mbid))
                .addToBackStack(AlbumsDetailsFragment.class.getName() + mbid)
                .commit();
    }

    @Override
    public void navigateToTrackDetails(String artist, String track, String mbid) {
        mFragmentManager.beginTransaction()
                .add(R.id.fragment_container, TrackDetailFragment.newInstance(artist, track, mbid))
                .addToBackStack(TrackDetailFragment.class.getName() + mbid)
                .commit();
    }

    @Override
    public void navigateToLoginVKDialog() {
        LoginVKDialogFragment.newInstance().show(mFragmentManager, LoginVKDialogFragment.class.getName());
    }

    @Override
    public void navigateBack() {
        int i = mFragmentManager.getBackStackEntryCount();
        if (i >= 1) {
            FragmentManager.BackStackEntry backEntry = mFragmentManager
                    .getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 1);
            String str = backEntry.getName();
            BaseFragment currentFragment = (BaseFragment) mFragmentManager.findFragmentByTag(str);
            if (currentFragment != null) {
                currentFragment.updateToolbar();
            }
        } else {
            mFirstFragment.updateToolbar();
        }
    }


    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            switch (MediaPlayerWrapper.getInstance().getCurrentState()) {
                case Retrieving:
                    mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                    break;
                case Stopped:
                    mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                    break;
                case Preparing:
                    mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                    break;
                case Playing:
                    mIvPlayPause.setImageResource(R.drawable.ic_pause_grey600_36dp);
                    break;
                case Paused:
                    mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                    break;
            }

            MediaPlayerWrapper.State state = MediaPlayerWrapper.getInstance().getCurrentState();
            if (!(state.equals(MediaPlayerWrapper.State.Retrieving)
                    || state.equals(MediaPlayerWrapper.State.Stopped) || state.equals(MediaPlayerWrapper.State.Preparing))) {
                long totalDuration = MediaPlayerWrapper.getInstance().getPlayerTotalDuration();
                long currentDuration = MediaPlayerWrapper.getInstance().getPlayerCurrentPosition();
                mTvTrackTotalDuration.setText("" + SeekBarUtils.milliSecondsToTimer(totalDuration));
                mTvCurrentTrackPosition.setText("" + SeekBarUtils.milliSecondsToTimer(currentDuration));
                mTvPlayTrackName.setText(MediaPlayerWrapper.getInstance().getCurrentTrack().getmTrackName());
                int progress = SeekBarUtils.getProgressPercentage(currentDuration, totalDuration);
                mSbSeekbar.setProgress(progress);
                Glide.with(MainActivity.this).load(MediaPlayerWrapper.getInstance().getCurrentTrack().getmAlbumImageUrl()).into(mIvAlbumsTrackImage);
            }

            mHandler.postDelayed(this, 100);
        }
    };


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (fromTouch) {
            int totalDuration = MediaPlayerWrapper.getInstance().getPlayerTotalDuration();
            int currentPosition = SeekBarUtils.progressToTimer(seekBar.getProgress(), totalDuration);
            MediaPlayerWrapper.getInstance().seekPlayerTo(currentPosition);
            mTvCurrentTrackPosition.setText("" + SeekBarUtils.milliSecondsToTimer(currentPosition));
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        updateProgressBar();
    }

    private void setupPlayerWidget() {
        switch (MediaPlayerWrapper.getInstance().getCurrentState()) {
            case Paused:
                mSmPlayer.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                updateProgressBar();
                break;
            case Playing:
                mSmPlayer.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                updateProgressBar();
                break;
            default:
                mSmPlayer.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                break;
        }

        mTrack = MediaPlayerWrapper.getInstance().getCurrentTrack().getmTrackName();
        mAlbumImage = MediaPlayerWrapper.getInstance().getCurrentTrack().getmAlbumImageUrl();
        mArtist = MediaPlayerWrapper.getInstance().getCurrentTrack().getmArtistName();
        mSbSeekbar.setOnSeekBarChangeListener(this);
        updateProgressBar();
        mIvPlayPause.setOnClickListener(view -> {
            mTrackUrl = MediaPlayerWrapper.getInstance().getCurrentTrack().getmTrackUrl();
            if (mTrackUrl != null) {
                MediaPlayerWrapper.getInstance().playTrack(mTrackUrl, mArtist, mTrack, mAlbumImage);
                switch (MediaPlayerWrapper.getInstance().getCurrentState()) {
                    case Retrieving:
                        mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                        break;
                    case Stopped:
                        mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                        break;
                    case Preparing:
                        mIvPlayPause.setImageResource(R.drawable.ic_pause_grey600_36dp);
                        break;
                    case Playing:
                        mIvPlayPause.setImageResource(R.drawable.ic_pause_grey600_36dp);
                        break;
                    case Paused:
                        mIvPlayPause.setImageResource(R.drawable.ic_play_grey600_36dp);
                        break;
                }
            }
        });
    }

    @Subscribe
    public void trackStartEvent(TrackPlayerEntity event) {
        mSmPlayer.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mTrack = event.getmTrackName();
        mAlbumImage = event.getmAlbumImageUrl();
        mArtist = event.getmArtistName();
        mTrackUrl = event.getmTrackUrl();
        mIvPlayPause.setImageResource(R.drawable.ic_pause_grey600_36dp);
        mTvPlayTrackName.setText(mTrack);
        Glide.with(this).load(mAlbumImage).into(mIvAlbumsTrackImage);
        updateProgressBar();
    }

    @Subscribe
    public void trackStartEvent(EventCurrentTrackPause eventCurrentTrackPause){
        mSmPlayer.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }
}
