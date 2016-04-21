package com.stafiiyevskyi.mlsdev.droidfm.view.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;

import com.stafiiyevskyi.mlsdev.droidfm.R;
import com.stafiiyevskyi.mlsdev.droidfm.view.Navigator;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.ArtistContentDetailsFragment;
import com.stafiiyevskyi.mlsdev.droidfm.view.fragment.ArtistSearchListFragment;

import butterknife.Bind;

public class MainActivity extends BaseActivity implements Navigator {
    @Bind(R.id.drawer_layout)
    DrawerLayout drNavigation;
    @Bind(R.id.nav_view)
    NavigationView nvNavigation;

    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getSupportFragmentManager();
        navigateToArtistsSearchScreen();

        nvNavigation.setNavigationItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.albums_item:
                    drNavigation.closeDrawers();
                    return true;
                case R.id.artists_item:
                    navigateToArtistsSearchScreen();
                    drNavigation.closeDrawers();
                    return true;
                case R.id.charts_item:
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
    public void navigateToArtistsSearchScreen() {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ArtistSearchListFragment.newInstance())
                .commit();
    }

    @Override
    public void navigateToArtistContentDetailsScreen(String mbid) {
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, ArtistContentDetailsFragment.newInstance(mbid))
                .addToBackStack(ArtistContentDetailsFragment.class.getName())
                .commit();
    }
}
