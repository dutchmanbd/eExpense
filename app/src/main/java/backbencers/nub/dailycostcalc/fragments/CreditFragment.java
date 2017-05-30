package backbencers.nub.dailycostcalc.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import backbencers.nub.dailycostcalc.R;
import backbencers.nub.dailycostcalc.adapter.CreditListAdapter;
import backbencers.nub.dailycostcalc.database.ExpenseDataSource;
import backbencers.nub.dailycostcalc.model.Credit;

/**
 * A simple {@link Fragment} subclass.
 */
public class CreditFragment extends Fragment {

    private static final String TAG = CreditFragment.class.getSimpleName();

    private static final int PERMISSION_CALLBACK_CONSTANT = 101;
    private static final int REQUEST_PERMISSION_SETTING = 102;

    private List<Credit> creditList;
    private ExpenseDataSource expenseDataSource;
    private ProgressBar loadingCreditProgressBar;
    private CreditListAdapter adapter;
    private ListView creditListView;
    private TextView creditEmptyView;
    private View view;

    private SharedPreferences permissionStatus;
    private boolean sentToSettings = false;

    // stopped here
    // http://www.androidhive.info/2016/11/android-working-marshmallow-m-runtime-permissions/

    public CreditFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.e(TAG, "onCreateView");

        getActivity().setTitle("Credit");

        return view = inflater.inflate(R.layout.fragment_credit, container, false);


        //readMessages();

        //creditList = expenseDataSource.getAllCredits();

        //CreditListAdapter adapter = new CreditListAdapter(getContext(), creditList);

        //creditListView.setAdapter(adapter);

        //new LoadCreditTask().execute();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        permissionStatus = getActivity().getSharedPreferences("permissionStatus", getActivity().MODE_PRIVATE);

        if (null != view) {
            creditListView = (ListView) view.findViewById(R.id.lv_credits);
            creditEmptyView = (TextView) view.findViewById(R.id.empty_view_credit);
            loadingCreditProgressBar = (ProgressBar) view.findViewById(R.id.pb_loding_credits);
            expenseDataSource = new ExpenseDataSource(getContext());

            creditListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    showCreditDetailInDialog(position);
                }
            });

            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_SMS)) {
                    //Show Information about why you need the permission
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Need Permission");
                    builder.setMessage("This app needs SMS permission.");
                    builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            requestPermissions(new String[]{Manifest.permission.READ_SMS}, PERMISSION_CALLBACK_CONSTANT);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                } else if (permissionStatus.getBoolean(Manifest.permission.READ_SMS, false)) {
                    //Previously Permission Request was cancelled with 'Dont Ask Again',
                    // Redirect to Settings after showing Information about why you need the permission
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Need Permission");
                    builder.setMessage("This app needs SMS permission.");
                    builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            sentToSettings = true;
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                            Toast.makeText(getActivity(), "Go to Permissions to Grant SMS", Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                } else {
                    //just request the permission
                    requestPermissions(new String[]{Manifest.permission.READ_SMS}, PERMISSION_CALLBACK_CONSTANT);
                }

                SharedPreferences.Editor editor = permissionStatus.edit();
                editor.putBoolean(Manifest.permission.READ_PHONE_STATE, true);
                editor.commit();
            } else {
                //You already have the permission, just go ahead.
                loadCredits();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CALLBACK_CONSTANT){
            //check if all permissions are granted
            boolean allgranted = false;
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i]==PackageManager.PERMISSION_GRANTED){
                    allgranted = true;
                } else {
                    allgranted = false;
                    break;
                }
            }


            if(allgranted){
                loadCredits();
            } else if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),Manifest.permission.READ_SMS)){
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Need SMS Permission");
                builder.setMessage("This app needs SMS permission.");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        requestPermissions(new String[]{Manifest.permission.READ_SMS},PERMISSION_CALLBACK_CONSTANT);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            } else {
                Toast.makeText(getActivity(),"Unable to get Permission",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                //Got Permission
                loadCredits();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();


        if (sentToSettings) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                //Got Permission
                loadCredits();
            }
        }
    }

    private void showCreditDetailInDialog(int position) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }
        Credit credit = (Credit) creditListView.getItemAtPosition(position);
        builder.setTitle(credit.getCreditDate())
                .setMessage(credit.getCreditDescription())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .show();
    }

    private void loadCredits() {
        loadingCreditProgressBar.setVisibility(View.VISIBLE);
        creditListView.setAdapter(new CreditListAdapter(getContext(), new ArrayList<Credit>()));
        expenseDataSource.deleteAllCredits();

        Credit credit = new Credit();
        credit.setCreditCategory("Bank");

        Cursor creditCursor = getActivity().getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (creditCursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String msgData = "";
                for (int idx = 0; idx < creditCursor.getColumnCount(); idx++) {
                    //msgData += " " + cursor.getColumnName(idx) + " : " + cursor.getString(idx) + "\n";
                    Timestamp timestamp;
                    Date date;

                    if (creditCursor.getColumnName(idx).equals("date")) {
                        timestamp = new Timestamp(creditCursor.getLong(idx));
                        date = new Date(timestamp.getTime());
                        String fullDateString = date.toString();
                        String monthDateString = fullDateString.substring(4, 10);
                        String yearString = fullDateString.substring(30, 34);
                        String dateString = monthDateString + ", " + yearString;
                        credit.setCreditDate(dateString);
                        //Log.i(CreditFragment.class.getSimpleName(), "Date: " + dateString);
                    }

                    if (creditCursor.getColumnName(idx).equals("body")) {
                        // TODO: add logic to add only the credit messages
                        String messageBodyNormal = creditCursor.getString(idx);
                        String messageBodyLowerCase = messageBodyNormal.toLowerCase();

                        if (messageBodyLowerCase.contains("credited") || messageBodyLowerCase.contains("cash in") || messageBodyLowerCase.contains("received")) {
                            Log.i(TAG, creditCursor.getString(idx));

                            credit.setCreditDescription(messageBodyNormal);

                            credit.setCreditAmount(findCreditAmountFromMessageBody(messageBodyLowerCase));

                            expenseDataSource.insertCredit(credit);
                        }
                    }

                }
                //Log.i(CreditFragment.class.getSimpleName(), credit.getCreditDate() + "\n\n");
                //pbLoading.setVisibility(View.INVISIBLE);
                // use msgData
//                if (msgData.toLowerCase().contains("credited")) {
//                    tvMessages.append(msgData + "\n\n\n");
//                }
            } while (creditCursor.moveToNext());
        } else {
            // empty box, no SMS
            //pbLoading.setVisibility(View.INVISIBLE);
            //tvMessages.append("\n\nNo messages found!\n\n");
        }

        try {
            if (creditCursor != null) {
                creditCursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            creditList = expenseDataSource.getAllCredits();
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadingCreditProgressBar.setVisibility(View.INVISIBLE);

        if (creditList.size() == 0) {
            creditEmptyView.setVisibility(View.VISIBLE);
        } else {
            adapter = new CreditListAdapter(getContext(), creditList);
            creditListView.setAdapter(adapter);
        }
    }

    private Double findCreditAmountFromMessageBody(String messageBody) {
        int indexOfTaka = -1;
        if (messageBody.contains("bdt")) {
            indexOfTaka = messageBody.indexOf("bdt");
        } else if (messageBody.contains("tk")) {
            indexOfTaka = messageBody.indexOf("tk");
        }

        double creditAmount = 0;
        if (indexOfTaka != -1) {
            for (int i = indexOfTaka; i < messageBody.length(); i++) {
                char c = messageBody.charAt(i);
                if (Character.isDigit(c)) {
                    int digit = (int) c - 48;
                    creditAmount = (creditAmount * 10) + digit;
                } else if (c == '.') {
                    break;
                }
            }
        }

        return creditAmount;
    }
}
