package com.example.text10;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {
    private final List<Msg> msgList = new ArrayList<>();
    private EditText inputText;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;

    public static final int TAKE_PHONE =1;
    private ImageView picture;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        initMsgs();//初始化消息数据
        Intent intent = getIntent();
        String data = intent.getStringExtra("receiver");
        String[] receiver = data.split("\n");
        ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.SEND_SMS},1);
        //获取SMSManager管理器
        final SmsManager smsManager = SmsManager.getDefault();

        //初始化控件
        inputText =(EditText) findViewById(R.id.input_text);
        Button send = (Button) findViewById(R.id.send);
        msgRecyclerView = (RecyclerView) findViewById(R.id.msg_recycler_view);
        Button takePhoto =(Button) findViewById(R.id.take_photo);
        picture =(ImageView) findViewById(R.id.picture);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);

        //获取短信 广播
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        MessageReceiver messageReceiver = new MessageReceiver();
        intentFilter.setPriority(100);
        registerReceiver(messageReceiver, intentFilter);

       //发送信息
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String contentText = inputText.getText().toString();
//                Log.d("contentText:",contentText);
                if(!"".equals(contentText)){
                    // 创建一个android.app.PendingIntent对象
//                    PendingIntent pi = PendingIntent.getActivity(SendActivity.this, 0, new Intent(), 0);
                    //发送短信
                    smsManager.sendTextMessage(receiver[1],null,contentText,null,null);
                    Msg msg = new Msg(contentText,Msg.TYPE_SENT);
                    msgList.add(msg);
                    //当有新消息的时候，刷新ListView中的显示
                    adapter.notifyItemInserted(msgList.size()-1);
                    //将ListView定位到最后一行
                    msgRecyclerView.scrollToPosition(msgList.size()-1);
                    inputText.setText("");//清空输入框中的内容
                }
            }
        });

        //拍照
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建File对象，用于存储拍照后的照片
                File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if(Build.VERSION.SDK_INT >= 24){
                    imageUri = FileProvider.getUriForFile(SendActivity.this,"com.example.text10.fileprovider",outputImage);
                }else {
                    imageUri = Uri.fromFile(outputImage);
                }
                //启动相机程序
                Intent intent1 = new Intent("android.media.action.IMAGE_CAPTURE");
                intent1.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent1,TAKE_PHONE);
            }
        });
    }

    class MessageReceiver extends BroadcastReceiver{
//        public String address = "";
//        Context context;
//
//        public MessageReceiver(Context context){
//            this.context = context;
//        }

        @Override
        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if(action.equals("android.provider.Telephony.SMS_RECEIVED")){
                Bundle bundle = intent.getExtras();
                Object[] pdus = (Object[]) bundle.get("pdus");//提取短信信息,有可能一次发来多条
                SmsMessage[] messages = new SmsMessage[pdus.length];
                for(int i =0;i< messages.length;i++){
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                String address = messages[0].getOriginatingAddress();//获取发送方的号码
                StringBuilder fullMessage = new StringBuilder();
                for (SmsMessage message : messages) {
                    fullMessage.append(message.getMessageBody());//获取短信内容
                }
                Msg msg = new Msg(fullMessage.toString(),Msg.TYPE_RECEIVED);
                msgList.add(msg);
                //当有新消息的时候，刷新ListView中的显示
                adapter.notifyItemInserted(msgList.size()-1);
                //将ListView定位到最后一行
                msgRecyclerView.scrollToPosition(msgList.size()-1);
                abortBroadcast();
//            }
        }
    }

    private void initMsgs(){
        Msg msg1 = new Msg("Hello guy.",Msg.TYPE_RECEIVED);
        msgList.add(msg1);
        Msg msg2 = new Msg("Hello.Who is that?",Msg.TYPE_SENT);
        msgList.add(msg2);
        Msg msg3 = new Msg("This is Tom.Nice talking to you.",Msg.TYPE_RECEIVED);
        msgList.add(msg3);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHONE:
                if (resultCode == RESULT_OK) {
                    try {
                        //将拍摄的照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }
}