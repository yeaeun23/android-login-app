package kr.co.seoul;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoginActivity extends AppCompatActivity {
    SharedPreferences pref;
    EditText idEt, pwEt;
    Button loginBtn;
    RelativeLayout mProgressLayout;
    String token, empno, nounce, pwd, sessionid;
    int loginResult = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = getSharedPreferences("session", 0);
        idEt = findViewById(R.id.id_et);
        pwEt = findViewById(R.id.pw_et);
        loginBtn = findViewById(R.id.login_btn);

        mProgressLayout = findViewById(R.id.progressLayout);
        mProgressLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        // 이전 아이디 불러오기
        idEt.setText(pref.getString("empno", ""));

        if (!idEt.getText().toString().equals("")) {
            pwEt.requestFocus(); // 커서 설정
        }

        // 비밀번호에서 엔터 클릭 이벤트
        pwEt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    new LoginTask().execute(idEt.getText().toString(), pwEt.getText().toString());
                    return true;
                }
                return false;
            }
        });

        // 로그인 버튼 클릭 이벤트
        loginBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                new LoginTask().execute(idEt.getText().toString(), pwEt.getText().toString());
            }
        });

        // 기기 토큰 구하기
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                token = instanceIdResult.getToken();
                Log.i("token", token);
            }
        });

//        // 버전 표시
//        try {
//            TextView verTv = findViewById(R.id.ver_tv);
//            verTv.setText("Ver. " + getPackageManager().getPackageInfo("kr.co.seoul", 0).versionName.substring(2));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkInstall();
        checkVersion();

        registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void checkInstall() {
        if (Build.VERSION.SDK_INT >= 26) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("앱 설치 알림");
                builder.setMessage("보안을 위해 '앱 설치 허용'을 설정해 주시기 바랍니다.");
                builder.setCancelable(false);
                builder.setPositiveButton("설정 화면으로 이동", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.show();
            }
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    public void checkVersion() {
        String LocalVer, ServerVer = "";

        try {
            // LocalVer 구하기
            LocalVer = getPackageManager().getPackageInfo("kr.co.seoul", 0).versionName;

            // ServerVer 구하기
            Map<String, String> map = new HashMap<>();
            ArrayList<Future<String>> results = new ArrayList<>();
            results.add(Executors.newCachedThreadPool().submit(new soapHandler("http://tempuri.com/checkVer", "checkVer", "https://mgate.seoul.co.kr/WebServiceS.asmx", map)));

            for (Future<String> fs : results) {
                ServerVer = fs.get();
            }

            // Ver 비교
            if (ServerVer.equals("false") || ServerVer.equals("")) {
                Toast.makeText(getApplicationContext(), "인터넷 연결을 확인하세요.", Toast.LENGTH_SHORT).show();
            } else if (Integer.parseInt(ServerVer) > Integer.parseInt(LocalVer)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("필수 업데이트 알림");
                builder.setMessage("앱 최신 버전이 있습니다. 업데이트 하시겠습니까?");
                builder.setCancelable(false);
                builder.setPositiveButton("업데이트", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        mProgressLayout.setVisibility(View.VISIBLE);
                        new getAPKThread(LoginActivity.this).start();
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver completeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mProgressLayout.setVisibility(View.INVISIBLE);

            Toast.makeText(context, "업데이트 파일 다운로드 완료", Toast.LENGTH_SHORT).show();
            finish();

            intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/download/mobilesis.apk")), "application/vnd.android.package-archive");
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider",
                        new File(Environment.getExternalStorageDirectory() + "/download/mobilesis.apk")), "application/vnd.android.package-archive");
            }

            startActivity(intent);
        }
    };

    private class LoginTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            mProgressLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... str) {
            String idStr = str[0];
            String pwStr = str[1];

            if (idStr.equals("")) {
                loginResult = 1;
            } else if (pwStr.equals("")) {
                loginResult = 2;
            } else if (idStr.length() != 7) {
                loginResult = 3;
            } else {
                try {
                    // empno 구하기
                    empno = idStr;
                    Log.i("empno", empno);

                    // nounce 구하기
                    JSONObject jsonObj1 = new JSONObject();
                    jsonObj1.put("id", empno);

                    nounce = new JSONObject(postUrl(jsonObj1, "getNounce")).get("d").toString();
                    Log.i("nounce", nounce);

                    // pwd 구하기
                    pwd = getMD5(getMD5(pwStr) + nounce);
                    Log.i("pwd", pwd);

                    // sessionid 구하기
                    JSONObject jsonObj2 = new JSONObject();
                    jsonObj2.put("nounce", nounce);
                    jsonObj2.put("empno", empno);
                    jsonObj2.put("pwd", pwd);

                    String getNounceResult = new JSONObject(postUrl(jsonObj2, "getAuth")).get("d").toString();
                    if (!getNounceResult.equals("-1")) {
                        sessionid = getNounceResult.split("∥")[1];
                        Log.i("sessionid", sessionid);

                        writeSharedPreference(empno, sessionid);

                        // 푸시 알림 설정
                        JSONObject jsonObj3 = new JSONObject();
                        jsonObj3.put("empno", empno);
                        jsonObj3.put("sessionid", sessionid);
                        jsonObj3.put("token", token);

                        Log.i("tokenResult", new JSONObject(postUrl(jsonObj3, "setPushID")).get("d").toString());

                        loginResult = 9;
                    } else {
                        loginResult = 4;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String str) {
            mProgressLayout.setVisibility(View.INVISIBLE);
            checkLoginResult(loginResult);
        }
    }

    public String getMD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());

            byte[] byteData = md.digest();
            StringBuilder sb = new StringBuilder();

            for (byte data : byteData) {
                sb.append(Integer.toString((data & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String postUrl(JSONObject jsonObj, String type) {
        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) new URL("https://mgate.seoul.co.kr/mobService/mobService.aspx/" + type).openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();

            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(jsonObj.toString());
            out.close();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                return sb.toString();
            } else {
                return urlConnection.getResponseMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }

        return null;
    }

    public void writeSharedPreference(String empno, String sessionid) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("empno", empno);
        editor.putString("sessionid", sessionid);
        editor.putString("expiredtime", getSessionExpiredTime());
        editor.apply();
    }

    public String getSessionExpiredTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, 20);

        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(cal.getTime());
    }

    public void checkLoginResult(int result) {
        switch (result) {
            case 1:
                Toast.makeText(getApplicationContext(), "사번을 입력하세요.", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(getApplicationContext(), "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(getApplicationContext(), "사번을 확인하세요.", Toast.LENGTH_SHORT).show();
                break;
            case 4:
                Toast.makeText(getApplicationContext(), "비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show();
                break;
            case 9:
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
                break;
            default:
                Toast.makeText(getApplicationContext(), "로그인 오류 발생! 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}