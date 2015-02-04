package com.zms.logcat;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


import com.zms.logcat.R;
import com.zms.logcat.R.id;
import com.zms.logcat.R.layout;
import com.zms.logcat.R.string;
import com.zms.logcat.prefs.Prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class FilterDialog extends AlertDialog {
    private boolean mError = false;
    private Prefs mPrefs;
    private Main mLogActivity;

    @Override
    public void dismiss() {
        if (!mError) {
            super.dismiss();
        }
    }

    public FilterDialog(Main logActivity) {
        super(logActivity);

        mLogActivity = logActivity;
        mPrefs = new Prefs(mLogActivity);

        LayoutInflater factory = LayoutInflater.from(mLogActivity);
        final View view = factory.inflate(R.layout.filter_dialog, null); //自定义对话框

        final EditText filterEdit = (EditText) view
                .findViewById(R.id.filter_edit);
        filterEdit.setText(mPrefs.getFilter());

        final TextView patternErrorText = (TextView) view.findViewById(R.id.pattern_error_text);
        patternErrorText.setVisibility(View.GONE);

        final CheckBox patternCheckBox = (CheckBox) view
                .findViewById(R.id.pattern_checkbox);
        patternCheckBox.setChecked(mPrefs.isFilterPattern());
        CompoundButton.OnCheckedChangeListener occl = new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (!isChecked) {
                    patternErrorText.setVisibility(View.GONE);
                    mError = false;
                }
            }

        };
        patternCheckBox.setOnCheckedChangeListener(occl);

        setView(view);
        setTitle(R.string.filter_dialog_title);

        setButton(BUTTON_POSITIVE, mLogActivity.getResources().getString(R.string.ok),
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FilterDialog fd = (FilterDialog) dialog;
                        String f = filterEdit.getText().toString();
                        if (patternCheckBox.isChecked()) {
                            try {
                                Pattern.compile(f);
                            } catch (PatternSyntaxException e) {
                                patternErrorText.setVisibility(View.VISIBLE);
                                fd.mError = true;
                                return;
                            }
                        }

                        fd.mError = false;
                        patternErrorText.setVisibility(View.GONE);

                        mPrefs.setFilter(filterEdit.getText().toString());
                        mPrefs.setFilterPattern(patternCheckBox.isChecked());

                        mLogActivity.setFilterMenu();//把过滤的条件显示在标题上
                        mLogActivity.dismissDialog(Main.FILTER_DIALOG);
                        mLogActivity.reset();
                    }
                });
        setButton(BUTTON_NEUTRAL, mLogActivity.getResources().getString(R.string.clear),
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FilterDialog fd = (FilterDialog) dialog;

                        mPrefs.setFilter(null);
                        filterEdit.setText(null);

                        mPrefs.setFilterPattern(false);
                        patternCheckBox.setChecked(false);

                        fd.mError = false;

                        mLogActivity.setFilterMenu();
                        mLogActivity.dismissDialog(Main.FILTER_DIALOG);
                        mLogActivity.reset();
                    }
                });
        setButton(BUTTON_NEGATIVE, mLogActivity.getResources().getString(R.string.cancel),
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FilterDialog fd = (FilterDialog) dialog;

                        filterEdit.setText(mPrefs.getFilter());
                        patternCheckBox.setChecked(mPrefs.isFilterPattern());

                        fd.mError = false;
                        mLogActivity.dismissDialog(Main.FILTER_DIALOG); //里面是id
                    }
                });

    }
}
