package com.ecarplug.ocas;

import com.ecarplug.ocas.network.MEApiInterface;
import com.ecarplug.ocas.network.WebApiInterface;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.cli.*;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
@Slf4j
public class Main
{
    // @formatter:off
    @Getter private static CustomApplication application = new CustomApplication();

    // @formatter:on

    /**
     * Options를 생성한다.
     *
     * @return
     */
    public static Options makeOptions()
    {
        Option debugMode = Option.builder("debug").desc("Debug Mode")
                .required(false).build();

        Option debugLog = Option.builder("debuglog").desc("Using Debugging Log")
                .required(false).build();

        Option ignoreMeApi = Option.builder("ignore_me").desc("Ignore ME API")
                .required(false).build();

        Option ignoreCRC = Option.builder("crc").desc("Ignore CRC Error")
                .required(false).longOpt("ignorecrc").build();

        Option port = Option.builder("p").hasArg(true).desc("Listen port")
                .required(false).type(Number.class).longOpt("port").build();

        Option refreshCharger = Option.builder("rc").hasArg(true).desc("Refresh Charger Info (min)")
                .required(false).type(Number.class).longOpt("refresh_charger").build();

		Option packetLogWriteMode = Option.builder("plw").hasArg(true).desc("Packet Log Write Mode")
				.required(false).type(Number.class).longOpt("packet_log_write").build();

		Option firmwareReadMode = Option.builder("fr").hasArg(true).desc("Firmware Read Mode")
				.required(false).type(Number.class).longOpt("firmware_read").build();

        Options options = new Options();
        options.addOption(port);
        options.addOption(refreshCharger);
        options.addOption(ignoreCRC);
        options.addOption(debugLog);
        options.addOption(debugMode);
        options.addOption(ignoreMeApi);
		options.addOption(packetLogWriteMode);
		options.addOption(firmwareReadMode);

        return options;
    }

    public static void main(String[] args)
    {
        Options options = makeOptions();

        // if(args.length < 1)
        // {
        //     // automatically generate the help statement
        //     HelpFormatter formatter = new HelpFormatter();
        //     formatter.printHelp("Ecarplug OCAS", options);
        //     return;
        // }

        CommandLineParser parser = new DefaultParser();

		// Date currentDT = new Date();
		// SimpleDateFormat transFormat = new SimpleDateFormat("yyMMddHHmmss");
		// TimeZone tz;
		// tz = TimeZone.getTimeZone("Asia/Seoul");
		// transFormat.setTimeZone(tz);
		// String strCurrentDT = transFormat.format(currentDT);
		// Byte payload[] = new Byte[6];
		// int payloadIdx = 0;
		// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(0, 2), 16);
		// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(2, 4), 16);
		// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(4, 6), 16);
		// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(6, 8), 16);
		// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(8, 10), 16);
		// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(10, 12), 16);
		//
		// Byte aa = Byte.parseByte("0", 16);
		// Byte bb = Byte.parseByte("00", 16);
		// Byte cc = 0x0;	// Byte.parseByte("0", 16);
		// Byte dd = 0;
		// Byte ee = Byte.parseByte("20", 16);
		// int i = 0;
		//
		// payload[0] = 0x0;
		// payload[1] = 0x0;
		// payload[2] = 0x0;
		// payload[3] = 0x0;
		// payload[4] = 0x0;
		// payload[5] = 0x0;


        try
        {
            CommandLine line = parser.parse(options, args);

            // String input = line.getOptionValue("i");
            // long size = (Long) line.getParsedOptionValue("s");
            // String[] ul = line.getOptionValues("ul");

            int port = 8085;
            if (line.hasOption("p")) {
                port = ((Number)line.getParsedOptionValue("p")).intValue();
                log.info(String.format("Enviroment [Port : %s]", port));
            }

            int refreshCharger = 10;    // 기본 10 분단위
            if (line.hasOption("rc")) {
                refreshCharger = ((Number)line.getParsedOptionValue("rc")).intValue();
            }
            log.info(String.format("Enviroment [Refresh Charger : %s min]", refreshCharger));

            boolean ignoreCRC = false;
            if (line.hasOption("crc"))
            {
                ignoreCRC = true;
            }
            log.info(String.format("Enviroment [Ignore CRC Error %s]", ignoreCRC ? "ENABLED" : "DISABLED"));

            boolean debugMode = false;
            if (line.hasOption("debug"))
            {
                debugMode = true;
            }
            log.info(String.format("Enviroment [Debug Mode %s]", debugMode ? "ENABLED" : "DISABLED"));

            boolean debugLog = false;
            if (line.hasOption("debuglog"))
            {
                debugLog = true;
            }
            log.info(String.format("Enviroment [Print Debug Log %s]", debugLog ? "ENABLED" : "DISABLED"));

            boolean ignoreMeApi = false;
            if (line.hasOption("ignore_me"))
            {
                ignoreMeApi = true;
            }
            log.info(String.format("Enviroment [Ignore ME API %s]", ignoreMeApi ? "ENABLED" : "DISABLED"));

			// sbNote 패킷 로그 저장방식, 0 사용안함, 1 웹서버, 2 Direct Insert, 3 Direct Insert and Query Batch Insert
			int packetLogWriteMode = 1;
			if (line.hasOption("plw")) {
				packetLogWriteMode = ((Number)line.getParsedOptionValue("plw")).intValue();
			}

			String hrPacketLogWriteMode = "";
			if (packetLogWriteMode == 0) hrPacketLogWriteMode = "Disable";
			else if (packetLogWriteMode == 1) hrPacketLogWriteMode = "Web API";
			else if (packetLogWriteMode == 2) hrPacketLogWriteMode = "Direct Insert";
			else if (packetLogWriteMode == 3) hrPacketLogWriteMode = "Direct Insert & Batch Insert";

			log.info(String.format("Enviroment [Packet Log Write Mode : %s]", hrPacketLogWriteMode));

			// sbNote 펌웨어 데이터 가져오는 방식, 1 웹서버, 2 로컬파일
			int firmwareReadMode = 1;
			if (line.hasOption("fr")) {
				firmwareReadMode = ((Number)line.getParsedOptionValue("fr")).intValue();
			}
			log.info(String.format("Enviroment [Firmware Read Mode : %s]", firmwareReadMode == 1 ? "Web API" : "Local"));

            application.setCommand(new ChargerCommand(application));
            application.setIgnoreCRC(ignoreCRC);
            application.setDebugMode(debugMode);
            application.setDebugLog(debugLog);
            application.setIgnoreMeApi(ignoreMeApi);
            application.setRefreshCharger(refreshCharger);

			application.setPacketLogWriteMode(packetLogWriteMode);
			application.setFirmwareReadMode(firmwareReadMode);

            // OkHttpClient okClient = new OkHttpClient.Builder()
            //         .addInterceptor(
            //                 chain -> {
            //                     Request.Builder builder = chain.request().newBuilder(); // Preference에서 cookies를 가져오는 작업을 수행
            //
            //                     SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(CustomApplication.getGlobalApplicationContext());
            //                     Set<String> preferences = sp.getStringSet("cookie", new HashSet<String>());
            //                     // Set<String> preferences = SharedPreferenceBase.getSharedPreference(APIPreferences.SHARED_PREFERENCE_NAME_COOKIE, new HashSet<String>());
            //
            //                     for (String cookie : preferences)
            //                     {
            //                         builder.addHeader("Cookie", cookie);
            //                     }
            //
            //                     // Web,Android,iOS 구분을 위해 User-Agent세팅
            //                     builder.removeHeader("User-Agent").addHeader("User-Agent", "Android EVRangCharger");
            //                     return chain.proceed(builder.build());
            //                 }).addInterceptor(
            //                 chain -> {
            //                     Response originalResponse = chain.proceed(chain.request());
            //                     if (!originalResponse.headers("Set-Cookie").isEmpty())
            //                     {
            //                         HashSet<String> cookies = new HashSet<>();
            //                         for (String header : originalResponse.headers("Set-Cookie"))
            //                         {
            //                             cookies.add(header);
            //                         }
            //
            //                         // Preference에 cookies를 넣어주는 작업을 수행
            //                         SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(CustomApplication.getGlobalApplicationContext());
            //                         SharedPreferences.Editor editor = sp.edit();
            //                         editor.putStringSet("cookie", cookies);
            //                         editor.apply();
            //                         // SharedPreferenceBase.putSharedPreference(APIPreferences.SHARED_PREFERENCE_NAME_COOKIE, cookies);
            //                     }
            //
            //                     return originalResponse;
            //                 }
            //         )
            //         .build();


            // Observable.interval(0, 5, TimeUnit.SECONDS)
            //         .subscribeOn(Schedulers.newThread())
            //         .subscribe(success -> {
            //
            //             application.getPacketLogQueue().offer(data -> {
            //
            //             });
            // });

            // Observable.fromIterable(_app.getPacketLogQueue())
            //         .subscribeOn(Schedulers.single())
            //         .subscribe(data -> {
            //             _app.getWebApi().packet_log(data)
            //                     .subscribeOn(Schedulers.trampoline())
            //                     // .subscribeOn(Schedulers.io())
            //                     .subscribe(success -> {
            //                         if (_app.isDebugLog())
            //                         {
            //                             log.debug(String.format("[%s-%s] Packet log send, 명령어 : %s, 타입 : %s, 응답 :  %s",
            //                                     data.get("sid"), data.get("cid"), data.get("ins"), data.get("type"), success.string()));
            //                         }
            //                     }, error -> {
            //                         log.error(data.get("ins") + ", " + data.get("type") + ", " + error.getMessage());
            //                     });
            //         });

			// sbNote DB 정보
			if (application.isDebugMode())
			{
				application.setDbHost("localhost");
				application.setDbPort("3306");
				application.setDbUserId("root");
				application.setDbUserPwd("ecarplug1!");
				application.setDbName("ecarplug");
			}
			else
			{
                application.setDbHost("localhost");
                application.setDbPort("3306");
                application.setDbUserId("root");
                application.setDbUserPwd("ecarplug1!");
                application.setDbName("ecarplug");
			}

			application.connectDB();

			InetAddress ip;
			try
			{
				ip = InetAddress.getLocalHost();
				application.setMyLocalIp(ip.getHostAddress());

			//	if (application.isDebugMode() && application.getMyLocalIp().equals("192.168.1.130"))
					application.setDeveloperServer(true);
			}
			catch (UnknownHostException e)
			{
				e.printStackTrace();
			}

			// Connection conn = null;
			// PreparedStatement pstmt = null;
			// ResultSet rs = null;
			//
			// try
			// {
			// 	Class.forName("org.mariadb.jdbc.Driver");
			//
			// 	conn = DriverManager.getConnection(
			// 			"jdbc:mariadb://" + application.getDbHost() + ":" + application.getDbPort() + "/" + application.getDbName(),
			// 			application.getDbUserId(),
			// 			application.getDbUserPwd());
			//
			// 	application.setDbConn(conn);
			//
			// 	// pstmt = conn.prepareStatement("select * from sb_codegroup");
			// 	// rs = pstmt.executeQuery();
			// 	// while (rs.next())
			// 	// {
			// 	// 	// cdg_id
			// 	// 	// cdg_name
			// 	// 	// cdg_code
			// 	// 	// cdg_desc
			// 	// 	// cdg_use_yn
			// 	// 	// cdg_write_datetime
			// 	// 	log.debug(rs.getInt("cdg_id") + " - " + rs.getString("cdg_name") + " - " + rs.getString("cdg_code"));
			// 	// 	// log.debug(rs.toString());
			// 	// }
			// }
			// catch (Exception e)
			// {
			// 	e.printStackTrace();
			// }
			// finally
			// {
			// 	// try
			// 	// {
			// 	// 	if (rs != null)
			// 	// 	{
			// 	// 		rs.close(); // 선택 사항
			// 	// 	}
			// 	//
			// 	// 	if (pstmt != null)
			// 	// 	{
			// 	// 		pstmt.close(); // 선택사항이지만 호출 추천
			// 	// 	}
			// 	//
			// 	// 	if (conn != null)
			// 	// 	{
			// 	// 		conn.close(); // 필수 사항
			// 	// 	}
			// 	// }
			// 	// catch (SQLException e)
			// 	// {
			// 	// 	e.printStackTrace();
			// 	// }
			// }

            // sbNote 환경부 API 정보
            application.setMeBid("EP");
            application.setMeBkey("EP1823eCgspcDalt");

            // sbDeploy 배포시 확인해야 사항입니다.
            String webServerUri = "https://test-manage.evrang.com";
            if (application.isDebugMode())
            {
                webServerUri = "https://test-manage.evrang.com";
                // webServerUri = "http://manage-dev.evrang.com:8080";
            }
			// sbDeploy =============================
            webServerUri = "http://localhost";


            application.setWebApiBaseUri(webServerUri);
            log.info(String.format("Enviroment [Web Server Uri : %s]", webServerUri));

            // sbNote EVRang 웹 API 호출
            ConnectionPool pool = new ConnectionPool(5, 10000, TimeUnit.MILLISECONDS);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectionPool(pool)
                    // .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(interceptor)
                    .build();

            // OkHttpClient okHttpClient = new OkHttpClient();

            application.setWebApi(new Retrofit.Builder()
                    .baseUrl(application.getWebApiBaseUri())
                    .client(okHttpClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(WebApiInterface.class));

            // sbNote 환경부 Rest API 호출
            ConnectionPool pool2 = new ConnectionPool(5, 10000, TimeUnit.MILLISECONDS);

            HttpLoggingInterceptor interceptor2 = new HttpLoggingInterceptor();
            interceptor2.setLevel(HttpLoggingInterceptor.Level.NONE);

            OkHttpClient okHttpClient2 = new OkHttpClient.Builder()
                    .connectionPool(pool2)
                    .addInterceptor(interceptor2)
                    .build();

            application.setMeApi(new Retrofit.Builder()
                    .baseUrl("http://10.101.160.34")
                    .client(okHttpClient2)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(MEApiInterface.class));

            // sbNote
            int finalPort = port;
            application.getWebApi().env_charger_list().subscribe(responseBody -> {
                try
                {
                    JSONObject res = new JSONObject(responseBody.string());
                    int responseStatus = res.getInt("status");

                    // JsonParser jsonParser = new JsonParser();
                    // JsonElement element = jsonParser.parse(responseBody.string());
                    // int responseStatus = element.getAsJsonObject().get("status").getAsInt();
                    // // int age = element.getAsJsonObject().get("age").getAsInt();
                    // // System.out.println("age = "+age);
                    //
                    // // Gson gson = new Gson();
                    // // gson.fromJson(element.getAsJsonObject().get("data"), aaa.class);
                    // // Person person = gson.fromJson(json, Person.class);
                    // // System.out.println("name = " + person.getName());
                    // // System.out.println("age = " + person.getAge());
                    // // System.out.println("gender = " + person.getGender());

                    if (responseStatus == 200)
                    {
                        // {
                        //     "status": 200,
                        //         "data": [
                        //         {
                        //             "chgs_name": "이카플러그_시험",
                        //                 "chgs_env_id": "190001",
                        //                 "chgs_station_id": "31-02-0006",
                        //                 "chg_charger_id": "31-02-0006-01",
                        //                 "chg_status": "0",
                        //                 "chg_active": "1"
                        //         },
                        //     ]
                        // }

                        // JSONObject data = res.getJSONObject("data");
                        //
                        // String deviceName = (data.has("station_id") ? data.getString("station_id") : "")
                        //         + "-"
                        //         + (data.has("charger_id") ? data.getString("charger_id") : "");
                        //
                        // // Date currentDT = new Date();
                        // // charger.setRecvChargerInfoLastTime(currentDT.getTime());
                        // charger.setRecvChargerInfoLastTime(System.currentTimeMillis());
                        //
                        // charger.setStrEnvStationId((data.has("env_station_id") ? data.getString("env_station_id") : ""));
                        //
                        // try
                        // {
                        //     charger.setRecvLastFirmwareVersion((data.has("charger_firmware_version") ? data.getString("charger_firmware_version") : ""));
                        //     charger.setRecvLastFirmwareLink((data.has("charger_firmware_link") ? data.getString("charger_firmware_link") : ""));
                        //     charger.setRecvLastFirmwareName((data.has("charger_firmware_name") ? data.getString("charger_firmware_name") : ""));
                        //     charger.setRecvLastFirmwareSize((data.has("charger_firmware_size") ? data.getInt("charger_firmware_size") : 0));
                        // }
                        // catch (Exception ex)
                        // {
                        //     log.error("[" + deviceName + "] ==> " + ex.getMessage());
                        // }
                        //
                        // charger.setActive((data.has("active") ? data.getInt("active") : 0));
                        // charger.setStatus((data.has("status") ? data.getInt("status") : 0));
                        //
                        // charger.setRecvChargerType((data.has("type") ? data.getString("type") : ""));
                        // charger.setRecvQuickTypeDcCombo((data.has("quick_dc_combo") && data.getString("quick_dc_combo").equals("1")));
                        // charger.setRecvQuickTypeCardemo((data.has("quick_type_cardemo") && data.getString("quick_type_cardemo").equals("1")));
                        // charger.setRecvQuickTypeAC3((data.has("quick_type_ac3") && data.getString("quick_type_ac3").equals("1")));
                        //
                        // charger.setRecvManufacturer((data.has("manufacturer") ? data.getString("manufacturer") : ""));
                        // charger.setRecvModelCode((data.has("model_code") ? data.getString("model_code") : ""));
                        // charger.setRecvConcurrentChannel((data.has("concurrent_channel") ? data.getInt("concurrent_channel") : 0));
                        // charger.setRecvFirmwareVersion((data.has("firmware_version") ? data.getString("firmware_version") : ""));
                        // charger.setRecvSound((data.has("sound") ? data.getInt("sound") : 0));
                        // charger.setRecvChannel((data.has("channel") ? data.getInt("channel") : 0));
                        //
                        // charger.setRecvKwh((data.has("kwh") ? data.getInt("kwh") : 0));                                             // 충전 요구 전력량 (H)
                        // charger.setRecvAmperage((data.has("amperage") ? data.getInt("amperage") : 0));                              // 충전 설정 전류량 (H)
                        //
                        // charger.setRecvAmperageChannel((data.has("amperage_channel") ? data.getInt("amperage_channel") : 0));               // 채널 설정 전류량 (D)
                        //
                        // ArrayList<Integer> unitPrice = new ArrayList<Integer>(24);
                        // JSONArray unitPriceJSONArray = data.has("unit_price") ? data.getJSONArray("unit_price") : new JSONArray();
                        // if (unitPriceJSONArray.length() == 24)
                        // {
                        //     for (int idx = 0; idx < 24; idx++)
                        //         unitPrice.add((int) unitPriceJSONArray.getInt(idx));
                        // }
                        //
                        // charger.setRecvUnitPrice(unitPrice);
                        //
                        // // log.info("[" + deviceName + "] ==> station id : " + data.getString("station_id"));
                        // // log.info("[" + deviceName + "] ==> charger id : " + data.getString("charger_id"));
                        // log.info("[" + deviceName + "] ==> 환경부 충전소 ID : " + charger.getStrEnvStationId());
                        //
                        // log.info("[" + deviceName + "] ==> last_firmware_version : " + charger.getRecvLastFirmwareVersion());
                        // log.info("[" + deviceName + "] ==> last_firmware_link : " + charger.getRecvLastFirmwareLink());
                        // log.info("[" + deviceName + "] ==> last_firmware_name : " + charger.getRecvLastFirmwareName());
                        // log.info("[" + deviceName + "] ==> last_firmware_size : " + charger.getRecvLastFirmwareSize());
                        //
                        // log.info("[" + deviceName + "] ==> charger type : " + data.getString("type"));
                        //
                        // log.info("[" + deviceName + "] ==> manufacturer : " + charger.getRecvManufacturer());
                        // log.info("[" + deviceName + "] ==> model code : " + charger.getRecvModelCode());
                        // log.info("[" + deviceName + "] ==> concurrent channel : " + charger.getRecvConcurrentChannel());
                        // log.info("[" + deviceName + "] ==> firmware version : " + charger.getRecvFirmwareVersion());
                        // log.info("[" + deviceName + "] ==> sound : " + charger.getRecvSound());
                        // log.info("[" + deviceName + "] ==> channel : " + charger.getRecvChannel());
                        //
                        // // Log.d(CustomApplication.TAG, "[" + deviceName + "] ==> 공급가능전력량 : " + getRecvAmperageSupplied());
                        // log.info("[" + deviceName + "] ==> 충전 요규 전력량 : " + charger.getRecvKwh());
                        // log.info("[" + deviceName + "] ==> 충전 설정 전류량 : " + charger.getRecvAmperage());
                        // log.info("[" + deviceName + "] ==> 채널 설정 전류량 : " + charger.getRecvAmperageChannel());
                        //
                        // log.info("[" + deviceName + "] ==> 충전단가 : " + charger.getRecvUnitPrice().toString());
                        //
                        // charger.setSuccessChargerInfo(true);
                        //
                        // emitter.onSuccess(new JSONObject());
                    }
                    else
                    {
                        // log.error(String.format("[%s-%s] 충전기 정보를 찾을수 없습니다.", stationID, chargerID));
                        //
                        // charger.setSuccessChargerInfo(false);
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                Main.startOcasServer(finalPort);

            }, error -> {
                Main.startOcasServer(finalPort);
            });
        }
        catch (ParseException e)
        {
            e.printStackTrace();

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Ecarplug OCAS", options);
        }
    }

    // sbTodo OCAS 구동시 EVRang 서버에서 충전기 정보를 가져오지 못한 경우...
    public static void startEnvUpdate()
    {
        Disposable timer = Observable.interval(0, 60 * 15, TimeUnit.SECONDS).subscribe(resolve -> {

        }, error -> {

        });

        application.setDisposableEnvTimer(timer);
    }

    public static void startOcasServer(int port)
    {
        // sbNote 서버 구동
        SimpleNettyServerBootstrap simpleNettyServerBootstrap = new SimpleNettyServerBootstrap();

        try
        {
            simpleNettyServerBootstrap.start(port);
            // simpleNettyServerBootstrap.startPush(8084);

            // simpleNettyServerBootstrap.start(port, 8084);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

	// public static void startPushServer(int port)
	// {
	// 	EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	// 	EventLoopGroup workerGroup = new NioEventLoopGroup();
	// 	ChannelFuture channelFuture = null;
	//
	// 	log.info("Starting push server at : " + port);
	//
	// 	try
	// 	{
	// 		ServerBootstrap b = new ServerBootstrap();
	// 		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
	// 				// .handler(new LoggingHandler(LogLevel.INFO))
	// 				.childHandler(new HttpServerInit());
	//
	// 		Channel ch = b.bind(port).sync().channel();
	//
	// 		channelFuture = ch.closeFuture();
	// 		channelFuture.sync();
	// 	}
	// 	catch (Exception e)
	// 	{
	// 		e.printStackTrace();
	// 	}
	// 	finally
	// 	{
	// 		bossGroup.shutdownGracefully();
	// 		workerGroup.shutdownGracefully();
	// 		log.info("Stopping push server");
	// 	}
	// }
}