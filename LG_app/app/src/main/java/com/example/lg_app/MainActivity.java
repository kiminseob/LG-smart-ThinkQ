package com.example.lg_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.annotation.TargetApi;
//UDP 관련
import org.w3c.dom.Text;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;


class MyView extends View implements Runnable{
 private Drawable image;

 int viewWidth, viewHeight;
 int imageWidth, imageHeight;
 int x, y;

     public MyView(Context context, AttributeSet attrs){
         super(context, attrs);
         image = this.getResources().getDrawable(R.drawable.drop_c3);
         Thread thread = new Thread(this);
         thread.start();
     }
     @Override
     protected void onSizeChanged(int w,int h,int oldw,int oldh){
         super.onSizeChanged(w, h,oldw,oldh);

         viewWidth = this.getWidth();
         viewHeight = this.getHeight();

         imageWidth = image.getIntrinsicWidth();
         imageHeight = image.getIntrinsicHeight();

         x = viewWidth/2 - imageWidth/2 +10;
         y = viewHeight/2 - (imageWidth+60);
     }
     @Override
    protected void onDraw(Canvas canvas){
         super.onDraw(canvas);

         image.setBounds(x,y,x+imageWidth,y+imageHeight);
         image.draw(canvas);
     }
     @Override
    public void run(){
        while(true){
                try{
                    Thread.sleep(5000);
                    for(int i=0; i<150; i++) {
                        Thread.sleep(100);
                        y += 10;
                        this.postInvalidate();
                    }
                    y=viewHeight/2- (imageWidth+60);

                }catch (Exception e){
                    e.printStackTrace();
                }

        }
     }
}

public class MainActivity extends AppCompatActivity{
    MyView mv;
    public static String getMonth="0"; //월 가져옴
    private ViewDate mViewDate = null; // 현재 시간 출력 쓰레드
    long now; //현재시간
    Date date; //날짜
    SimpleDateFormat sdf = new SimpleDateFormat("M");

    float real_humidity;
    String show_battery;
    float real_temperature;
    int month;
    private SendData mSendData = null; // 통신 쓰레드

    //서버주서
    public static final String sIP = "119.203.57.191";
    //사용할 통신 포트
    public static final int sPORT = 50007;
    public static String msg2="0";
    public static String battery2="loading";
    public static String temperature2="0";

    public static String[] separate_msg2;
    public static String[] separate_battery2;

    public HumidityUpdate HU = null;

    //화면 표시용 TextView
    public TextView humidity = null;
    public TextView battery = null;
    public TextView temperature = null;
    public ImageView manual = null;
    public TextView standardH = null;
    public TextView standardT = null;
    public ImageView season = null;

    public TextView humidity_state = null;
    public TextView temperature_state = null;
    public TextView humidity_state2 = null;
    public ConstraintLayout background_image = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewDate = new ViewDate(); //현재 월 가져옴.
        mViewDate.start();
        mSendData = new SendData(); //로딩화면 전에 먼저 쓰레드활성화(습도, 배터리 받아온다.)
        mSendData.start();


        Intent intent = new Intent(this, introActivity.class); //로딩화면 가져옴
        startActivity(intent); //로딩화면 실행
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //풀스크린


        mv = (MyView)findViewById(R.id.mv); // mv 캔버스에 드롭할 방울의 뷰

        //Button humidity_bt = (Button) findViewById(R.id.Humidity_bt); //습도 버튼을 가져온다.
        humidity = (TextView) findViewById(R.id.textView); //습도
        battery = (TextView) findViewById(R.id.textView2); //배터리
        temperature = (TextView) findViewById(R.id.textView5); //온도
        temperature_state = (TextView)findViewById(R.id.textView10); //밑에 온도
        humidity_state =  (TextView) findViewById(R.id.textView3); // 습도 퍼센트 상태
        humidity_state2 =  (TextView) findViewById(R.id.textView4); // 습도 상태
        manual = (ImageView)findViewById(R.id.imageView);  // 메뉴얼 페이지 이동
        background_image = (ConstraintLayout)findViewById(R.id.main_background);//메인의 백그라운드 배경설정

        standardH = (TextView)findViewById(R.id.standard_humiduty); //적정습도
        standardT = (TextView)findViewById(R.id.standard_temperature);  //적정온도
        season = (ImageView)findViewById(R.id.season); //계절별 아이콘 가져옴옴

       HU = new HumidityUpdate(); //습도와 배터리를 업데이트하는 쓰레드
        HU.start();

       humidity.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {  //습도 텍스트를 터치하면 메뉴얼 페이지로 넘어간다.
                Intent intent = new Intent(getApplicationContext(), intend_activity.class);
                startActivity(intent);
            }
        });
        manual.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {  //습도 텍스트를 터치하면 메뉴얼 페이지로 넘어간다.
                Intent intent = new Intent(getApplicationContext(), ManualActivity.class);
                startActivity(intent);
            }
        });

    }
    public void humidity_level(float HL){
        if(HL>90) {
            background_image.setBackgroundResource(R.drawable.humidity100);
        }
        else if(HL>80) {
            background_image.setBackgroundResource(R.drawable.humidity90);
        }
        else if(HL>70) {
            background_image.setBackgroundResource(R.drawable.humidity80);
        }
        else if(HL>60) {
            background_image.setBackgroundResource(R.drawable.humidity70);
        }
        else if(HL>50) {
            background_image.setBackgroundResource(R.drawable.humidity60);
        }
        else if(HL>40) {
            background_image.setBackgroundResource(R.drawable.humidity50);
        }
        else if(HL>30) {
            background_image.setBackgroundResource(R.drawable.humidity40);
        }
        else if(HL>20) {
            background_image.setBackgroundResource(R.drawable.humidity30);
        }
        else if(HL>10) {
            background_image.setBackgroundResource(R.drawable.humidity20);
        }
        else{
            background_image.setBackgroundResource(R.drawable.humidity10);
        }
    }

    //계절별 월 구하기 (봄3~5 여름6~8 가을9~11 겨울12~2)
    class ViewDate extends Thread{
        public void run(){
            try {
                while (true) {
                    now = System.currentTimeMillis();
                    date = new Date(now);
                    getMonth = sdf.format(date); //현재 몇월인지 가져옴.

                    Thread.sleep(5000);
                }
            }catch (Exception e){
                Log.v("tag :","오류");
            }
        }
    }
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
                    msg2 = new String(packet.getData()); //습도

                    separate_msg2 = msg2.split(":");
                    msg2 = separate_msg2[0];
                    battery2 = separate_msg2[1];
                    temperature2 = separate_msg2[2];


                    if(!battery2.equals("loading") &&battery2.length()>2){
                        separate_battery2 = battery2.split("d");
                        battery2 = separate_battery2[0].replaceAll("\\s+","");
                    }
                    String temp="";
                    String temp2= separate_msg2[2].replaceAll("\\s+","");

                    if(temp2.length()>5) {
                        for (int i = 0; i < temp2.length(); i++) {
                            if (Character.isLetter((temp2.charAt(i)))) {
                                for (int j = 0; j < i; j++) {
                                    temp += temp2.charAt(j);
                                }
                                break;
                            }
                        }
                        temperature2=temp;
                    }
                    else {
                        temperature2 = temp2;
                    }
                    Thread.sleep(5000); // 1/1000단위 1초 == 1000, 1분 == 60000
                }
            }catch (Exception e){
                Log.v("tag :","오류");
            }
        }


    }


//습도와 배터리를 update하여 화면에 출력하는 쓰레드
    class HumidityUpdate extends Thread{
        public void run(){
            try{
                while(true) {
                    /*SharedPreferences prefs = getSharedPreferences("Humidity", MODE_PRIVATE); //백그라운드 딕셔너리 습도 가져옴
                    show_humidity = prefs.getString("Humidity", "0");
                    real_humidity = Float.parseFloat(show_humidity);
                    SharedPreferences prefs2 = getSharedPreferences("Battery", MODE_PRIVATE); //백그라운드 딕셔너리 배터리 가져옴
                    show_battery = prefs2.getString("Battery", "loading");
                    */
                    real_humidity = Float.parseFloat(msg2);
                    show_battery = battery2;
                    real_temperature= Float.parseFloat(temperature2);
                    month = Integer.parseInt(getMonth);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            humidity_level(real_humidity); //습도별 배경설정(습도 게이지)
                            humidity_state.setText(real_humidity + "%");
                            temperature_state.setText(real_temperature+"℃");


                             //(봄3~5 여름6~8 가을9~11 겨울12~2)
                             //봄 + 가을
                            if( (month>=3 && month<=5) || (month>=9 && month<=11) ){

                                standardH.setText("적정 습도\n45~55%");
                                standardT.setText("적정 온도\n19~23℃");
                                if((month>=9 && month<=11)) {
                                    season.setBackgroundResource(R.drawable.fall);
                                }
                                else{
                                    season.setBackgroundResource(R.drawable.spring);
                                }
                                 //습도0 온도x
                                if(real_humidity>=45 && real_humidity<=55){
                                    if(real_temperature<19){
                                        humidity_state2.setText("습도는 적당하나\n온도를 조금 올리면 좋겠네요. :(");
                                    }
                                    else if(real_temperature>23){
                                        humidity_state2.setText("습도는 적당하나\n온도를 조금 내리면 좋겠네요. :(");
                                    }
                                }
                                //온도0 습도x
                                if(real_temperature>=19 && real_temperature<=23){
                                    if(real_humidity<45){
                                        humidity_state2.setText("온도는 적당하나\n습도를 조금 올리면 좋겠네요. :(");
                                    }
                                    else if(real_humidity>55){
                                        humidity_state2.setText("온도는 적당하나\n습도를 조금 내리면 좋겠네요. :(");
                                    }
                                }
                                //습도0 온도0
                                if(real_humidity>=45 && real_humidity<=55){
                                    if(real_temperature>=19 && real_temperature<=23){
                                        humidity_state2.setText("정말 쾌적한 환경입니다.\n기분 좋은 하루 보내세요 :)");
                                    }
                                }
                                if(real_humidity<45 || real_humidity>55) {
                                    if(real_temperature<19 || real_temperature>23) {
                                        humidity_state2.setText("온습도가 기준치에 맞지 않네요 :(\n적절한 관리를 해주세요.");
                                    }
                                }


                            }
                            //여름
                            if(month>=6 && month <=8){
                                Log.v("tag","7");
                                standardH.setText("적정 습도\n55~65%");
                                standardT.setText("적정 온도\n24~27℃");
                                season.setBackgroundResource(R.drawable.sun);

                                //습도0 온도x
                                if(real_humidity>=55 && real_humidity<=65){
                                    if(real_temperature<24){
                                        humidity_state2.setText("습도는 적당하나\n온도를 조금 올리면 좋겠네요. :(");
                                    }
                                    else if(real_temperature>27){
                                        humidity_state2.setText("습도는 적당하나\n온도를 조금 내리면 좋겠네요. :(");
                                    }
                                }
                                //온도0 습도x
                                if(real_temperature>=24 && real_temperature<=27){
                                    if(real_humidity<55){
                                        humidity_state2.setText("온도는 적당하나\n습도를 조금 올리면 좋겠네요. :(");
                                    }
                                    else if(real_humidity>65){
                                        humidity_state2.setText("온도는 적당하나\n습도를 조금 내리면 좋겠네요. :(");
                                    }
                                }
                                //습도0 온도0
                                if(real_humidity>=55 && real_humidity<=65){
                                    if(real_temperature>=24 && real_temperature<=27){
                                        humidity_state2.setText("정말 쾌적한 환경입니다.\n기분 좋은 하루 보내세요 :)");
                                    }
                                }
                                if(real_humidity<55 || real_humidity>65) {
                                    if(real_temperature<24 || real_temperature>27) {
                                        humidity_state2.setText("온습도가 기준치에 맞지 않네요 :(\n적절한 관리를 해주세요.");
                                    }
                                }
                            }

                            //겨울
                            if(month>=12 ||month<=2){
                                Log.v("tag","8");
                                standardH.setText("적정 습도\n35~45%");
                                standardT.setText("적정 온도\n18~21℃");
                                season.setBackgroundResource(R.drawable.winter);
                                //습도0 온도x
                                if(real_humidity>=35 && real_humidity<=45){
                                    if(real_temperature<18){
                                        humidity_state2.setText("습도는 적당하나\n온도를 조금 올리면 좋겠네요. :(");
                                    }
                                    else if(real_temperature>21){
                                        humidity_state2.setText("습도는 적당하나\n온도를 조금 내리면 좋겠네요. :(");
                                    }
                                }
                                //온도0 습도x
                                if(real_temperature>=18 && real_temperature<=21){
                                    if(real_humidity<35){
                                        humidity_state2.setText("온도는 적당하나\n습도를 조금 올리면 좋겠네요. :(");
                                    }
                                    else if(real_humidity>45){
                                        humidity_state2.setText("온도는 적당하나\n습도를 조금 내리면 좋겠네요. :(");
                                    }
                                }
                                //습도0 온도0
                                if(real_humidity>=35 && real_humidity<=45){
                                    if(real_temperature>=18 && real_temperature<=21){
                                        humidity_state2.setText("정말 쾌적한 환경입니다.\n기분 좋은 하루 보내세요 :)");
                                    }
                                }
                                if(real_humidity<35 || real_humidity>45) {
                                    if(real_temperature<18 || real_temperature>21) {
                                        humidity_state2.setText("온습도가 기준치에 맞지 않네요 :(\n적절한 관리를 해주세요.");
                                    }
                                }

                            }



                            humidity.setText(real_humidity + "%");
                            temperature.setText(temperature2+"℃");
                            if(show_battery.equals("loading")){
                                battery.setText("load");
                            }

                            else{
                                battery.setText(show_battery+"%");
                            }
                            //Log.v("tag",msg);

                        }

                    });
                    Thread.sleep(3000);
                }
             }catch (Exception e){
                humidity.setText("오류");
            }

        }

    }


}