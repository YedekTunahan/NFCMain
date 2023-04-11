
package com.umut.soysal;


import android.app.PendingIntent;
import android.content.Intent;

import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;


import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.DG11File;

import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;

import org.jmrtd.lds.LDS;

import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.PACEInfo;


import java.util.Arrays;

import java.util.Collection;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    private View mainLayout;
    private View loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    @Override
    protected void onResume() {
        super.onResume();

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            Intent intent = new Intent(getApplicationContext(), this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String[][] filter = new String[][]{new String[]{"android.nfc.tech.IsoDep"}};
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }



    @Override
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {

            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);

            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {


                BACKeySpec bacKey = new BACKey("A40U47500","970103","330127");
                new ReadTask(IsoDep.get(tag), bacKey).execute();

            }
        }
    }


   ///NFC ile okuma bilgilerin servisten alınması
    private class ReadTask extends AsyncTask<Void, Void, Exception> {

        private IsoDep isoDep;
        private BACKeySpec bacKey;

        public ReadTask(IsoDep isoDep, BACKeySpec bacKey) {
            this.isoDep = isoDep;
            this.bacKey = bacKey;
        }

        private DG1File dg1File;
        private DG2File dg2File; //MRZ içerisindeki kisisel bilgiler alınır . Ad soyad gibi
        private DG11File dg11File; // Adres ve aytıntılı bilgiler
        private Bitmap bitmap;

        @Override
        protected Exception doInBackground(Void... params) {
            try {

                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                PassportService service = new PassportService(cardService);
                service.open();

                boolean paceSucceeded = false; // pace başarılı oldu. Test ortamıdır

           /*     try {
                    CardAccessFile cardAccessFile = new CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS)); // CardAccessFile  - biometrik veriler tutar , resim , imze , parmak izi
                    // okuma , doğrulama için kullanılır

                    Collection<PACEInfo> paceInfos = cardAccessFile.getPACEInfos();//hız bilgileri
                    if (paceInfos != null && paceInfos.size() > 0) {
                        PACEInfo paceInfo = paceInfos.iterator().next();

                        // PACE, elektronik pasaportlarda kimlik doğrulama ve anahtar değişimi işlemlerini gerçekleştirmek için kullanılan bir protokoldür.
                        // kimlik doğrulama işlemi sırasında kullanılan nesne tanımlayıcısını döndürür.
                        //Bu işlem, pasaportta bulunan güvenlik anahtarlarının doğru bir şekilde kullanılmasını sağlar ve verilerin güvenliği için önemlidir.
                        //doPace() metodu, PACE işleminin başlatılması ve tamamlanması için gereken adımları gerçekleştirir.
                        // Bu adımlar arasında pasaportun kimlik doğrulama anahtarlarına erişmek, PACE verilerini oluşturmak, PACE işlemini yürütmek ve sonuçları doğrulamak yer alabilir.

                        service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()));
                        paceSucceeded = true;
                    } else {
                        paceSucceeded = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }*/

                Log.e("1","1");
                service.sendSelectApplet(paceSucceeded);
                Log.e("2","2");
                if (!paceSucceeded) {
                    try {
                        Log.e("3","3");
                        service.getInputStream(PassportService.EF_COM).read(); // hata burada
                        Log.e("4","4");
                    } catch (Exception e) {
                        Log.e("5","5");
                        Log.e("G", String.valueOf(bacKey));
                        service.doBAC(bacKey);  // Patladığımız yer
                        Log.e("ç", String.valueOf(bacKey));
                        Log.e("6","6");
                    }
                }

                Log.e("7","7");
                LDS lds = new LDS();
                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                lds.add(PassportService.EF_DG1, dg1In, dg1In.getLength());
                dg1File = lds.getDG1File();
                Log.e("dg1File", String.valueOf(dg1File));

            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {

            if (result == null) {

                MRZInfo mrzInfo = dg1File.getMRZInfo();

                Log.e("Alınan MRZ",mrzInfo.getPrimaryIdentifier());

            } else {
                //Snackbar.make(passportNumberView, exceptionStack(result), Snackbar.LENGTH_LONG).show();
            }
        }

    }

}
