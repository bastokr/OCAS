package com.ecarplug.ocas.network;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.*;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * evRang Rest API 연결용 인터페이스
 */
public interface WebApiInterface
{
    @POST("/api/rest_info/env_charger_list")
    Single<ResponseBody> env_charger_list();

    @FormUrlEncoded
    @POST("/api/rest_ocas/user_info")
    Single<ResponseBody> ocas_user_info(@FieldMap ConcurrentHashMap<String, Object> param);

    @FormUrlEncoded
    @POST("/api/rest_ocas/payment_available")
    Single<ResponseBody> ocas_payment_available(@FieldMap ConcurrentHashMap<String, Object> param);

    @FormUrlEncoded
    @POST("/api/rest_ocas/payment")
    Single<ResponseBody> ocas_payment(@FieldMap ConcurrentHashMap<String, Object> param);

	@FormUrlEncoded
	@POST("/api/rest_ocas/save_prepay_payment")
	Single<ResponseBody> ocas_save_prepay_payment(@FieldMap ConcurrentHashMap<String, Object> param);

	@FormUrlEncoded
	@POST("/api/rest_ocas/charger_state")
	Single<ResponseBody> ocas_charger_state(@FieldMap ConcurrentHashMap<String, Object> param);

    @FormUrlEncoded
    @POST("/api/rest_log/packet_log")
    Single<ResponseBody> packet_log(@FieldMap ConcurrentHashMap<String, Object> param);



    // @GET("/ajax/appmanager/kakao_login")
    // Single<ResponseBody> kakao_login(@Query("id") long kakaoId,
    //                                  @Query("nickname") String kakaoNickname,
    //                                  @Query("email") String kakaoEmail,
    //                                  @Query("profile_image") String kakaoProfileImage,
    //                                  @Query("thumbnail_image") String kakaoThumbnailImage);

    @GET("/ajax/appmanager/login")
    Single<ResponseBody> login(@Query("id") String id,
                                    @Query("pwd") String pwd);

    // @GET("/ajax/appmanager/check_id")
    // Single<ResponseBody> check_id(@Query("id") String id);

    // @GET("/ajax/appmanager/register")
    // Single<ResponseBody> register(@Query("type") String type,
    //                               @Query("id") String id,
    //                               @Query("name") String name,
    //                               @Query("pwd") String pwd,
    //                               @Query("phone") String phone,
    //                               @Query("email") String email);
    //
    // @GET("/ajax/appmanager/findpwd")
    // Single<ResponseBody> findpwd(@Query("id") String id,
    //                              @Query("phone") String phone,
    //                              @Query("email") String email);

    @GET("/ajax/appmanager/member_info")
    Single<ResponseBody> member_info(@Query("login_type") String login_type,
                                     @Query("login_id") String login_id,
                                     @Query("social_id") long kakaoId);

    @GET("/ajax/appmanager/list_charging_history")
    Single<ResponseBody> list_charging_history(@Query("login_type") String login_type,
                                               @Query("login_id") String login_id,
                                               @Query("social_id") long kakaoId);

    @GET("/ajax/appmanager/list_point")
    Single<ResponseBody> list_point(@Query("login_type") String login_type,
                                    @Query("login_id") String login_id,
                                    @Query("social_id") long kakaoId);

    @GET("/ajax/appmanager/point_use")
    Single<ResponseBody> point_use(@Query("login_type") String login_type,
                                   @Query("login_id") String login_id,
                                   @Query("social_id") long kakaoId,
                                   @Query("point") long point,
                                   @Query("order_no") String order_no);

    @GET("/ajax/firmware/last_firmware_all")
    Single<ResponseBody> getLastFirmware();

    @GET("/ajax/station/charger_info")
    Single<ResponseBody> getChargerInfo(@Query("id") String id);

    @GET("/ajax/station/charger_certification")
    Single<ResponseBody> getChargerCertification(
            @Query("station_id") String stationId,
            @Query("charger_id") String chargerId,
            @Query("charger_dt") String chargerDt);

    @GET("/ajax/appmanager/put_charging_history")
    Single<ResponseBody> putChargingLog(
            @Query("login_type") String loginType,
            @Query("login_id") String userId,
            @Query("social_id") long kakaoId,
            @Query("station_id") String stationId, @Query("charger_id") String chargerId,
            @Query("channel") int channel, @Query("order_user_no") String orderUserNo,
            @Query("start") String startDT, @Query("end") String endDT, @Query("unplug") String unplugDT,
            @Query("kwh") String kwh);




}