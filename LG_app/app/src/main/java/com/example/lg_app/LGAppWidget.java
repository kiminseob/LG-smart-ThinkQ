package com.example.lg_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

/**
 * Implementation of App Widget functionality.
 */
public class LGAppWidget extends AppWidgetProvider {
    public static ComponentName mService = null; //서비스 클래스 시작을 위해 ComponentName 형 변수 선언.

    //서버주서
    public static final String sIP = "서버 주소";
    //사용할 통신 포트
    public static final int sPORT = 50007;
    public static int curCount=0;
    public static boolean endServiceFlag=false;
    public static String msg="0";
    public static String battery="loading";
    public static String temperature="0";
    public static String getTime="0";
    public static String getHour="0";
    public static String[] separate_msg;
    public static String[] separate_battery;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        endServiceFlag = false;
        mService = context.startService(new Intent(context, UpdateService.class));

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static class UpdateService extends Service  implements Runnable{
        private Handler mHandler; //서비스를 일정시간 이후 깨울때 필요함.
        private static final int TIMER_PERIOD = 60000; //서비스는 기본적으로 밀리세컨드 단위이므로 1초는 1000ms 1분=60000
        private long preTime; //이전시간과 현재 시간을 비교하여 일정 차이보다 작으면 뷰를 갱신한다.
        private long curTime; //현재시간
        private SendData mSendData = null; // 통신 쓰레드

        @Override
        public void onCreate(){
            mHandler = new Handler(); //핸들러를 생성하여 일정시간 후에 run()함수가 호출될 수 있도록 한다.
        }

        @Override
        public void onStart(Intent intent, int startId){
            mSendData = new SendData();

            mSendData.start();

            preTime = System.currentTimeMillis();//현재 시간을 이전 시간에 저장 후
            mHandler.postDelayed(this, 500);   //10초뒤에 서비스클래스의 run()함수가 호출되도록 한다.
        }

        @Override
        public IBinder onBind(Intent intent) {
            // TODO Auto-generated method stub
            return null;
        }
        /*
         * 위젯 화면 업데이트를 주기적으로 하기위해 run 함수 호출.
         */
        @Override
        public void run() {
            //run 함수 내에서 갱신 작업을 처리한다.
            //리모트뷰에 앱위젯의 레이아웃을 결합한다.
            RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.lgapp_widget);
            //이미지 아이콘 누르면 앱 페이지 출력.
            Intent intent=new Intent(this, MainActivity.class);
            PendingIntent pe= PendingIntent.getActivity(this, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.appwidget_text, pe);
            /*
            * 지난 업데이트로 부터 시간이 약 5초가  지나면 text뷰의 습도를 업데이트한다.
            */
            curTime = System.currentTimeMillis();//현재 시간을 이전 시간에 저장 후  

            long CUR_PERIOD = curTime - preTime;
            if( CUR_PERIOD > 500 ){
                /*SharedPreferences prefs = getSharedPreferences("Humidity", MODE_PRIVATE);//데이터 공유 딕셔너리
                SharedPreferences.Editor editor = prefs.edit(); //딕셔너리 에디터
                editor.putString("Humidity", msg);
                editor.apply(); // 데이터 백그라운드 저장
                SharedPreferences prefs2 = getSharedPreferences("Battery", MODE_PRIVATE);//데이터 공유 딕셔너리
                SharedPreferences.Editor editor2 = prefs2.edit(); //딕셔너리 에디터
                editor2.putString("Battery", battery);
                editor2.apply();

                if(Integer.parseInt(getHour)>=0 &&Integer.parseInt(getHour)<=11) {
                    views.setTextViewText(R.id.appwidget_text2, "오후 "+getTime);
                }
                else{
                    views.setTextViewText(R.id.appwidget_text2, "오전 "+getTime);
                }*/
                if(msg.equals("0")){
                    views.setTextViewText(R.id.appwidget_text, "");
                }
                else {
                    views.setTextViewText(R.id.appwidget_text,   msg + "%");//+Integer.toString(curCount));
                }
                views.setTextViewText(R.id.appwidget_text3,temperature+"℃");
                preTime = curTime;
                curCount++;
            }

            //위젯이 업데이트 되었음을 알린다.
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName testWidge = new ComponentName(this, LGAppWidget.class);
            appWidgetManager.updateAppWidget(testWidge, views);

            if(endServiceFlag){

            }else{
                // 다음번에 run()함수는 TIMER_PERIOD 후에 호출된다.
                mHandler.postDelayed(this, TIMER_PERIOD);
            }
        } //run end!!!

        class SendData extends Thread{
            public void run(){

                try{
                    while(true) {
                        //UDP 통신용 소켓 생성
                        DatagramSocket socket = new DatagramSocket();

                        //서버 주소 변수
                        InetAddress serverAddr = InetAddress.getByName(sIP);

                        //보낼 데이터 생성
                        byte[] buf = ("send me data please ").getBytes();

                        //패킷으로 변경
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, sPORT);

                        //패킷 전송!
                        socket.send(packet);
                        //데이터 수신 대기
                        socket.receive(packet);
                        //데이터 수신되었다면 문자열로 변환
                        msg = new String(packet.getData()); //습도+배터리+온도


                        separate_msg = msg.split(":");
                        msg = separate_msg[0];
                        battery = separate_msg[1];
                        temperature = separate_msg[2];
                        Log.v("잇힝1 :",msg);
                        Log.v("잇힝2 :",battery);
                        Log.v("잇힝3 :",temperature);
                        if(!battery.equals("loading") &&battery.length()>2){
                            separate_battery = battery.split("d");
                            battery = separate_battery[0].replaceAll("\\s+","");
                        }
                        String temp="";
                        String temp2= separate_msg[2].replaceAll("\\s+","");

                        if(temp2.length()>5) {
                            for (int i = 0; i < temp2.length(); i++) {
                                if (Character.isLetter((temp2.charAt(i)))) {
                                    for (int j = 0; j < i; j++) {
                                        temp += temp2.charAt(j);
                                    }
                                    break;
                                }
                            }
                            temperature = temp;
                        }
                        else {
                            temperature = temp2;
                        }
                        //Log.v("잇힝 :",temperature);

                        Thread.sleep(60000); // 1/1000단위 1초 == 1000, 1분 == 60000
                    }
                }catch (Exception e){
                    Log.v("tag :","오류");
                }
            }


        }



    }// inner class end



}

