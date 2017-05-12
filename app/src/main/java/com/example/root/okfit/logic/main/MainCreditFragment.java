package com.example.root.okfit.logic.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.root.okfit.R;
import com.example.root.okfit.logic.SecondActivity;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by PengFeifei on 17-5-11.
 */

public final class MainCreditFragment extends MainFragment {

    @BindView(R.id.text)
    TextView text;

    @Override
    protected void init(Bundle savedInstanceState, View view) {
        text.setText("MainCreditFragment");
    }

    @OnClick(R.id.text)
    protected void toBubble() {
        startActivity(new Intent(getActivity(), SecondActivity.class));
    }

    @Override
    protected int getContentViewId() {
        return R.layout.frament_main_credit;
    }
}