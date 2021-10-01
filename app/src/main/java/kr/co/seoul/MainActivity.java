package kr.co.seoul;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    SharedPreferences pref;
    WebView mWebView;
    RelativeLayout mProgressLayout;
    long backPressedTime = 0;
    //boolean backFlag = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getSharedPreferences("session", 0);
        mWebView = findViewById(R.id.webView);

        mProgressLayout = findViewById(R.id.progressLayout);
        mProgressLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mProgressLayout.setVisibility(View.VISIBLE);

        // 헤더 설정
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("empno", pref.getString("empno", ""));
        extraHeaders.put("sessionid", pref.getString("sessionid", ""));

        // 웹뷰 설정
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    view.getContext().startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
                } else if (url.startsWith("http:") || url.startsWith("https:")) {
                    // 외부 사이트 또는 이미지가 아닌 첨부파일일 경우, 웹 브라우저로 열기
                    if (!url.contains("mgate.seoul.co.kr") || url.endsWith(".zip") || url.endsWith(".hwp") || url.endsWith(".pdf") ||
                            url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".xls") || url.endsWith(".xlsx") ||
                            url.endsWith(".ppt") || url.endsWith(".pptx") || url.endsWith(".mp3") || url.endsWith(".mp4")) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } else {
                        mProgressLayout.setVisibility(View.VISIBLE);
                        view.loadUrl(url);
                    }
                }
                // 화면 확대/축소 기능 설정
                if (url.contains("seoulcokr-") || url.contains("ctp/Images")) {
                    view.getSettings().setSupportZoom(true);
                    view.getSettings().setBuiltInZoomControls(true);
                    view.getSettings().setDisplayZoomControls(false);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // 이전, 다음, 새로고침 버튼 눌렀을 때 필요
                mProgressLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressLayout.setVisibility(View.INVISIBLE);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setBackgroundColor(0x00000000);
        mWebView.getSettings().setJavaScriptEnabled(true);                      // JS 허용 여부
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false); // JS 새창 띄우기(멀티뷰) 허용 여부
        mWebView.getSettings().setSupportMultipleWindows(false);                // 새창 띄우기 허용 여부
        mWebView.getSettings().setLoadWithOverviewMode(true);                   // 메타 태그 허용 여부
        mWebView.getSettings().setUseWideViewPort(true);                        // 화면 사이즈 맞추기 허용 여부
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);         // 캐시 허용 여부
        mWebView.getSettings().setDomStorageEnabled(false);                     // 로컬 저장소 허용 여부
        mWebView.loadUrl("https://mgate.seoul.co.kr/mobsis/Home.aspx?start=1", extraHeaders);
    }

    @Override
    public void onResume() {
        super.onResume();

        // 세션 만료 시간 체크
        try {
            if (System.currentTimeMillis() > new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).parse(pref.getString("expiredtime", "")).getTime()) {
                Toast.makeText(getApplicationContext(), "세션이 만료되었습니다.", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        String url = mWebView.getUrl();

        if (url.contains("Home.aspx")) {
            if (System.currentTimeMillis() > backPressedTime + 2000) {
                backPressedTime = System.currentTimeMillis();
                Toast.makeText(getApplicationContext(), "뒤로 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        } else {
            mProgressLayout.setVisibility(View.VISIBLE);

            if (url.contains("BoardDetail.aspx")) {
                mWebView.loadUrl(url.replace("BoardDetail", "BoardList"));
            } else if (url.contains("ApprList.aspx") || url.contains("ApprGongmun.aspx") || url.contains("BoardList.aspx") || url.contains("Family.aspx") ||
                    url.contains("MyunList.aspx") || url.contains("SMS.aspx") || url.contains("Work.aspx") || url.contains("Settings.aspx")) {
                mWebView.loadUrl("https://mgate.seoul.co.kr/mobsis/Home.aspx");
            } else {
                mWebView.goBack();
            }
        }

        // 화면 확대/축소 기능 설정
        mWebView.getSettings().setSupportZoom(false);
        mWebView.getSettings().setBuiltInZoomControls(false);

//        Toast toast = Toast.makeText(getApplicationContext(), "뒤로 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT);

//        if (mWebView.canGoBack()) {
//            if (System.currentTimeMillis() > backPressedTime + 2000) {
//                backPressedTime = System.currentTimeMillis();
//                backFlag = false;
//
//                mWebView.goBack();
//            } else {
//                if (!backFlag) {
//                    backPressedTime = System.currentTimeMillis();
//                    backFlag = true;
//
//                    toast.show();
//                } else {
//                    finish();
//                }
//            }
//        } else {
//            if (System.currentTimeMillis() > backPressedTime + 2000) {
//                backPressedTime = System.currentTimeMillis();
//
//                toast.show();
//            }
//            else {
//                finish();
//            }
//        }
    }
}
