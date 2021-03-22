package com.ecarplug.ocas.network;


import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 환경부 REST API 연결용 인터페이스
 */
public interface MEApiInterface
{
    @FormUrlEncoded
    @POST("/r1/charger/status/update")
    Single<ResponseBody> charger_status(@FieldMap ConcurrentHashMap<String, Object> param);
}