package com.stafiiyevskyi.mlsdev.droidfm.data.api;

import com.stafiiyevskyi.mlsdev.droidfm.data.dto.artist.SearchArtist;
import com.stafiiyevskyi.mlsdev.droidfm.data.dto.artist.TopChartArtists;
import com.stafiiyevskyi.mlsdev.droidfm.data.dto.tag.TopChartTags;

import java.util.Map;

import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import rx.Observable;

/**
 * Created by oleksandr on 20.04.16.
 */
public interface LastFMService {


    @GET("?method=chart.gettopartists")
    Observable<TopChartArtists> getTopChartArtist(@Query("page") int pageNumber, @QueryMap Map<String, String> queryAdditional);

    @GET("?method=chart.gettoptags")
    Observable<TopChartTags> getTopChartTags(@Query("page") int pageNumber, @QueryMap Map<String, String> queryAdditional);

    @GET("?method=artist.search")
    Observable<SearchArtist> searchArtist(@Query("name") String searchName, @QueryMap Map<String, String> queryAdditional);
}
