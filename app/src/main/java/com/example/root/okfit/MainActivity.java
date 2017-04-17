package com.example.root.okfit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.crnetwork.dataformat.DrList;
import com.example.crnetwork.dataformat.DrRoot;
import com.example.crnetwork.host.BaseUrlBindHelper;
import com.example.crnetwork.host.ServerType;
import com.example.crnetwork.response.DrResponse;
import com.example.crnetwork.response.ResponseCallback;
import com.example.root.okfit.bean.breakers.BreakerItem;
import com.example.root.okfit.bean.errors.ErrorItem;
import com.okfit.repository.ClassTestRepository;
import com.okfit.repository.MethodTestRepository;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private TextView textView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BaseUrlBindHelper.resetBaseUrl(ServerType.PRODUCT);

        textView = (TextView) this.findViewById(R.id.text);
        textView2 = (TextView) this.findViewById(R.id.text2);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMethodBreakers();
                getClassErrors();
            }
        });
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMethodAsyncBreakers();
                getClassAsyncErrors();
            }
        });
    }

    private void log(String tag, String log) {
        Log.e("TAG-->" + tag, log);
    }

    private void getMethodBreakers() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DrList<BreakerItem> drList = new MethodTestRepository().getBreakers("android");
                log("getMethodBreakers", drList.getList().get(0).getName());
            }
        }).start();
    }

    private void getMethodAsyncBreakers() {
        new MethodTestRepository().getBreakers("ios", new ResponseCallback<DrRoot<DrList<BreakerItem>>>() {
            @Override
            public void onResponse(Call<DrRoot<DrList<BreakerItem>>> call, Response<DrRoot<DrList<BreakerItem>>> response) {
                super.onResponse(call, response);
                ArrayList<BreakerItem> listData = new DrResponse<DrRoot<DrList<BreakerItem>>>().getListData(response, call);
                log("getMethodAsyncBreakers", listData.get(1).getName());
            }
        });
    }


    private void getClassErrors() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DrList<ErrorItem> drList = new ClassTestRepository().getErros();
                log("getClassErrors", drList.getList().get(0).getZhCN());
            }
        }).start();
    }

    private void getClassAsyncErrors() {
        new ClassTestRepository().getErros(new ResponseCallback<DrRoot<DrList<ErrorItem>>>() {
            @Override
            public void onResponse(Call<DrRoot<DrList<ErrorItem>>> call, Response<DrRoot<DrList<ErrorItem>>> response) {
                super.onResponse(call, response);
                ArrayList<ErrorItem> listData = new DrResponse<DrRoot<DrList<ErrorItem>>>().getListData(response, call);
                log("getClassAsyncErrors", listData.get(1).getZhCN());

            }
        });
    }


}
