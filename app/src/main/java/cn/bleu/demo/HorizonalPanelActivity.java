package cn.bleu.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.want.base.sdk.framework.app.fragment.MListFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Project:</b> BleuStudio<br>
 * <b>Create Date:</b> 16/1/8<br>
 * <b>Author:</b> Gordon<br>
 * <b>Description:</b> <br>
 */
public class HorizonalPanelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slidemenu);

        final MListFragment<String> fragment = new MListFragment<String>() {
            @Override
            protected View onCreateItemView(LayoutInflater inflater, int pos, String data) {
                return inflater.inflate(android.R.layout.activity_list_item, null);
            }

            @Override
            protected void onUpdateItemView(View view, int pos, String data) {
                final TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setText(data);
            }
        };

        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
        getSupportFragmentManager().executePendingTransactions();

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                fragment.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Toast.makeText(HorizonalPanelActivity.this,
                                       "data " + fragment.getDataSet().get(position),
                                       Toast.LENGTH_SHORT).show();
                    }
                });


                List<String> datas = new ArrayList<String>();
                for (int i = 0; i < 100; i++) {
                    datas.add("data: " + i);
                }

                fragment.updateData(datas);
            }

        });

    }
}
