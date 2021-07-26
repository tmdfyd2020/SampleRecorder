package org.techtown.samplerecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ShortBuffer;

public class WaveForm extends SurfaceView implements SurfaceHolder.Callback{
    int ScreenWidth,ScreenHeight,BoardWidth,BoardHeight;
    int BoardStartX,BoardStartY,BoardMiddleWidth,
            BoardMiddleHeight,BoardEndX,BoardEndY;

    Canvas canvas;

    double RatioX,RatioY;
    int TimeDiv,MaxHeight, VRange;
    // AudioRecord로 부터 받은 데이터의 수
    int DataLength;
    ShortBuffer Buffer, tempBuffer, ReadBuffer;
    SurfaceHolder mHolder;

    AudioRecord audioRecord;
    boolean isData;

    public WaveForm(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveForm(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WaveForm(Context context) {  // 도대체 이 context가 어떤 context를 말하는겅?
        super(context);
        init();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        myLog.d("method activate");

        mHolder = holder;
        getScreenInfo();
        canvas = holder.lockCanvas();
        drawBoard();
        holder.unlockCanvasAndPost(canvas);
        //audioRecord = new AudioRecord(this);
        //audioRecord.setBoardManager(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void init(){
        myLog.d("method activate");
        SurfaceHolder mHolder=getHolder();
        mHolder.addCallback(this);

        Buffer = ShortBuffer.allocate(MainActivity.SampleRate);
        tempBuffer = ShortBuffer.allocate(MainActivity.SampleRate);

        //화면에 표시할 시간
        TimeDiv=1000;

        //표시할 최대값
        MaxHeight=32767;
    }

    // 화면 크기 및 출력 지점 등의 좌표를 설정
    public void getScreenInfo(){
        myLog.d("method activate");
        // 화면의 크기를 얻어옴
        ScreenWidth = getWidth();
        ScreenHeight = getHeight();

        // 박스의 크기를 화면의 90%크기로 설정
        BoardWidth=(int)(ScreenWidth*0.9);
        BoardHeight=(int)(ScreenHeight*0.9);

        // 박스의 그릴 지점 설정
        BoardStartX=(ScreenWidth-BoardWidth)/2;
        BoardStartY=(ScreenHeight-BoardHeight)/2;
        BoardEndX=BoardStartX+BoardWidth;
        BoardEndY=BoardStartY+BoardHeight+2;

        // 박스의 중앙선
        BoardMiddleHeight=BoardHeight/2;
        BoardMiddleWidth=BoardWidth/2;

        //표시 배율
        RatioY=(BoardHeight-2)/((6-VRange)*MaxHeight*2.0f);
        RatioX=(BoardWidth-2)*1000/(double)(TimeDiv);

        isData=false;
    }

    public void setData(ShortBuffer readBuffer, int dataLength){
        myLog.d("method activate");
        this.ReadBuffer = readBuffer;
        myLog.d(String.valueOf(readBuffer));
        this.DataLength = dataLength;

        canvas = mHolder.lockCanvas();  // 이젠 여기에서 걸림
        drawBoard();
        drawData();
        mHolder.unlockCanvasAndPost(canvas);
        isData = true;

        // 여기에 있는 무언가를 실행해서 null 포인터인것이 아닌것 같다.
        // 그냥 저쪽에서 문제인듯
    }

    public void drawBoard(){
        myLog.d("method activate");
        Paint paint = new Paint();

        //배경화면 힌색으로
        paint.setColor(Color.WHITE);
        canvas.drawRect(1,1,ScreenWidth,ScreenHeight,paint);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);

        //보드 외곽선
        canvas.drawRect(BoardStartX-1,BoardStartY,BoardEndX+2,BoardEndY,paint);

        //보드 십자선
        paint.setStrokeWidth(1);
        canvas.drawLine(BoardStartX+BoardMiddleWidth,BoardStartY,
                BoardStartX+BoardMiddleWidth,BoardEndY,paint);
        canvas.drawLine(BoardStartX,BoardStartY+BoardMiddleHeight
                ,BoardEndX,BoardStartY+BoardMiddleHeight,paint);

    }

    public void drawData(){
        myLog.d("method activate");
        double data, Stime, Ttime;
        int x, y, i;

        // AudioRecord 에서 입력된 데이터가 있을 때만 그림
        if (isData==true) {
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(5);

            tempBuffer.position(0);
            tempBuffer.put(Buffer.array(),0,MainActivity.SampleRate-1);//버퍼 복사
            Buffer.position(0);
            //앞부분을 readBuffer의 내용으로 채움
            Buffer.put(ReadBuffer.array(),0,DataLength-1);
            //readBuffer의 내용 뒷부분을 원래의 값으로 채움-> 쉬프트
            Buffer.put(tempBuffer.array(),0,MainActivity.SampleRate-DataLength-1);

            //샘플링 한주기의 시간을 구함
            Stime=1.0f/MainActivity.SampleRate;
            //그려야할 전체 시간(1초) - TimeDiv/1000
            Ttime=TimeDiv/(Stime*1000);

            //데이터를 읽어와서 화면에 출력
            Buffer.position(0);
            for (i=0;i<(int)Ttime;i++) {
                //그려질 x 좌표 구하기
                x = (int) ((i + 1) * Stime * RatioX) + BoardStartX + 1;

                //y값 구하고 역상만들기 및 아래로 내리기
                data = -RatioY * Buffer.get(i);
                y = BoardMiddleHeight + (int) data + BoardStartY;

                //그리기
                canvas.drawPoint(x, y, paint);
            }
            isData=false;
        }
    }
}
