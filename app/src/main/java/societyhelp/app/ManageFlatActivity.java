package societyhelp.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import societyhelp.app.util.CustomSerializer;
import societyhelp.dao.SocietyHelpDatabaseFactory;
import societyhelp.dao.mysql.impl.Flat;
import societyhelp.dao.mysql.impl.Login;

public class ManageFlatActivity extends DashBoardActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_flat);
        setHeader("", true, false);
        List<Flat> flats = new ArrayList<>();
        try {
            byte[] sObjet = (byte[]) getIntent().getSerializableExtra(CONST_ALL_FLATS);
            flats = (List<Flat>) CustomSerializer.deserializeObject(sObjet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            final TableLayout tableL = (TableLayout) findViewById(R.id.allFlatTableLayout);
            tableL.invalidate();

            for (Flat l : flats) {
                TableRow row = new TableRow(this);
                TextView fId = new TextView(this); fId.setText(l.flatId);row.addView(fId);
                TextView fNo = new TextView(this); fNo.setText(l.flatNumber);row.addView(fNo);
                TextView fArea = new TextView(this); fArea.setText(String.valueOf(l.area));row.addView(fArea);
                TextView fMaintenanceAmount = new TextView(this); fMaintenanceAmount.setText(String.valueOf(l.maintenanceAmount));row.addView(fMaintenanceAmount);
                TextView fBlock = new TextView(this); fBlock.setText(l.block);row.addView(fBlock);
                TextView fStatus = new TextView(this); fStatus.setText(Boolean.toString(l.status));row.addView(fStatus);
                tableL.addView(row);
            }
        }catch (Exception e)
        {
            Log.e("Error", "Flat list fetch has problem", e);
        }
    }
}
