package com.ecarplug.ocas;

import com.ecarplug.ocas.model.ChargerModel;
import com.ecarplug.ocas.network.MEApiInterface;
import com.ecarplug.ocas.network.WebApiInterface;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.disposables.Disposable;
import lombok.Setter;
import lombok.Getter;
import retrofit2.SkipCallbackExecutor;
import retrofit2.http.GET;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class CustomApplication
{
	@Getter @Setter String onlyTestTime = "200603124416";

	@Getter @Setter private String myLocalIp = "";
	@Getter @Setter private boolean developerServer = true;
    @Getter @Setter private boolean debugMode = true;
    @Getter @Setter private boolean debugLog = true;
    @Getter @Setter private boolean ignoreMeApi = true;

    @Getter @Setter private boolean sendNCmdHistory = true;

    @Getter @Setter private boolean ignoreCRC = false;
    @Getter @Setter private int refreshCharger = 60;

    // sbNote 패킷 로그 저장방식, 0 사용안함, 1 웹서버, 2 Direct Insert, 3 Direct Insert and Query Batch Insert
    @Getter @Setter private int packetLogWriteMode = 0;
    // sbNote 펌웨어 데이터 가져오는 방식, 1 웹서버, 2 로컬파일
    @Getter @Setter private int firmwareReadMode = 0;

	@Getter @Setter private Connection dbConn = null;
	@Getter @Setter private String dbHost;
	@Getter @Setter private String dbPort;
	@Getter @Setter private String dbUserId;
	@Getter @Setter private String dbUserPwd;
	@Getter @Setter private String dbName;

    @Getter @Setter private String webApiBaseUri;
    @Getter @Setter private WebApiInterface webApi;
    @Getter @Setter private MEApiInterface meApi;

    @Getter @Setter private Disposable disposableEnvTimer = null;

    @Getter @Setter private String meBid;
    @Getter @Setter private String meBkey;

    @Getter @Setter private ChargerCommand command;

    @Getter private ConcurrentHashMap<String, ChargerModel> clientMap = new ConcurrentHashMap<String, ChargerModel>(10);
	@Getter private ConcurrentMap<String, ConcurrentMap<String, ChannelHandlerContext>> userChannelMap = new ConcurrentHashMap<String, ConcurrentMap<String, ChannelHandlerContext>>(10);

    // @Getter private ConcurrentLinkedQueue<Object> packetLogQueue = new ConcurrentLinkedQueue<>();
    // @Getter private BlockingQueue<ConcurrentHashMap<String, Object>> packetLogQueue = new ArrayBlockingQueue<>(100);
    // @Getter private Executor logExecutor = Executors.newFixedThreadPool(30);
    // @Getter private Executor stateExecutor = Executors.newFixedThreadPool(1);

	public void connectDB()
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			Class.forName("org.mariadb.jdbc.Driver");

			conn = DriverManager.getConnection(
					"jdbc:mariadb://" + getDbHost() + ":" + getDbPort() + "/" + getDbName(),
					getDbUserId(),
					getDbUserPwd());

			setDbConn(conn);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			// try
			// {
			// 	if (rs != null)
			// 	{
			// 		rs.close(); // 선택 사항
			// 	}
			//
			// 	if (pstmt != null)
			// 	{
			// 		pstmt.close(); // 선택사항이지만 호출 추천
			// 	}
			//
			// 	if (conn != null)
			// 	{
			// 		conn.close(); // 필수 사항
			// 	}
			// }
			// catch (SQLException e)
			// {
			// 	e.printStackTrace();
			// }
		}
	}
}
