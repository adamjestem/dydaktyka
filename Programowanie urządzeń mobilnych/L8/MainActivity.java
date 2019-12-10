package android.mybluetoothmsg;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private boolean CONTINUE_READ_WRITE = true;
    private boolean CONNECTION_ESTABLISHED = false;

    Button onBT, offBT, visibleBT, listDevBT, sendBT, openBT, closeBT;
    EditText msgET;
    CheckBox serverCB;
    ListView devMsgLV;

    ArrayList<String> listMsgItems;
    ArrayAdapter<String> listAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream is;
    private OutputStream os;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice remoteDevice;

    // nazwa i UUID stanowiace identyfikator aplikacji
    private static String NAME = "bluetoothapp";
    private static UUID MY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        onBT = (Button) findViewById(R.id.onBT);
        offBT = (Button) findViewById(R.id.offBT);
        visibleBT = (Button) findViewById(R.id.visibleBT);
        listDevBT = (Button) findViewById(R.id.listDevBT);
        msgET = (EditText) findViewById(R.id.messagePT);
        sendBT = (Button) findViewById(R.id.sendBT);
        openBT = (Button) findViewById(R.id.openBT);
        closeBT = (Button) findViewById(R.id.closeBT);
        serverCB = (CheckBox) findViewById(R.id.serverCB);
        serverCB.setChecked(true);
        devMsgLV = (ListView) findViewById(R.id.devMsgLV);

        listMsgItems = new ArrayList<String>(); // ArrayLista gromadzaca wszystkie wiadomosci
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listMsgItems);
        devMsgLV.setAdapter(listAdapter);

        // akcja klikniecia na item LV
        devMsgLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String name = (String) parent.getItemAtPosition(position);
                for(BluetoothDevice bt : pairedDevices) { //foreach
                    if(name.equals(bt.getName())) {
                        remoteDevice = bt;
                        Toast.makeText(getApplicationContext(), "Selected " + remoteDevice.getName(), Toast.LENGTH_SHORT ).show();
                    }
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { // Jesli adapter==null to brak Bluetooth
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        list(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnection(null);
    }


    /**
     * Uruchamianie modulu Bluetooth
     * @param v
     */
    public void on(View v) {
        if (!bluetoothAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Wylaczenie modulu Bluetooth
     * @param v
     */
    public void off(View v) {
        bluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
    }

    /**
     * Wlaczenie widocznosci urzadzenia Bluetooth
     * @param v
     */
    public void visible(View v) {
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    /**
     * Wyszukiwanie i wylistowanie sparowanych urzadzen Bluetooth
     * @param v
     */
    public void list(View v) {
        listMsgItems.clear(); // usuwanie historii czatu
        listAdapter.notifyDataSetChanged();

        pairedDevices = bluetoothAdapter.getBondedDevices(); // pozyskanie sparowanych urzadzen

        for(BluetoothDevice bt : pairedDevices) { // dodanie urzadzen do ListView
            listMsgItems.add(0, bt.getName());
        }
        listAdapter.notifyDataSetChanged(); // przeladowanie interfejsu
    }

    /**
     * Uruchomienie watku klienta badz serwera
     * @param v
     */
    public void openConnection(View v) {
        // zabezpieczenia:
        if(bluetoothAdapter == null) {
            bluetoothAdapter = getDefaultAdapter(); // utworzenie adaptera
        }
        if (!bluetoothAdapter.isEnabled()) {
            on(null); // uruchomienie modulu Bluetooth
        }

        CONTINUE_READ_WRITE = true;
        // resetowanie soketu i strumieni WE/WY jesli byly wczesniej uzywane
        socket = null;
        is = null;
        os = null;

        if(pairedDevices.isEmpty() || remoteDevice == null) {
            Toast.makeText(this, "Paired device is not selected, choose one", Toast.LENGTH_SHORT).show();
            return;
        }

        if(serverCB.isChecked()) { // watek SERWERA
             new Thread(serverListener).start();
        }
        else { // watek KLIENTA
            new Thread(clientConnecter).start();
        }
    }


    /**
     * Zamykanie otwartych polaczen i czyszczenie zasobow
     * @param v
     */
    public void closeConnection(View v) {
        CONTINUE_READ_WRITE = false;
        CONNECTION_ESTABLISHED = false;

        if (is != null) { // zamkniecie strumienia WE
            try {is.close();} catch (Exception e) {}
            is = null;
        }

        if (os != null) { // zamkniecie strumienia WY
            try {os.close();} catch (Exception e) {}
            os = null;
        }

        if (socket != null) { // zamkniecie socketa
            try {socket.close();} catch (Exception e) {}
            socket = null;
        }

        try { // zamykanie watkow
            Handler mHandler = new Handler();
            mHandler.removeCallbacksAndMessages(writter);
            mHandler.removeCallbacksAndMessages(serverListener);
            mHandler.removeCallbacksAndMessages(clientConnecter);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(), "Communication closed" ,Toast.LENGTH_SHORT).show();

        list(null); // wyswietlenie listy wyboru urzadzenia
        msgET.setText("Write a message here...");
    }

    /**
     * watek serwera
     */
    private Runnable serverListener = new Runnable()
    {
        public void run()
        {
            try{ // otwarcie polaczenia Bluetooth
                socket =(BluetoothSocket) remoteDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(remoteDevice,1);
                socket.connect();
                CONNECTION_ESTABLISHED = true; // polaczenia ustanowione

            }
            catch(Exception e) { // starsza metoda otwierania polaczenia BT (dla starszych urzadzen)
                try {
                    BluetoothServerSocket tmpsocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                    socket = tmpsocket.accept();
                    CONNECTION_ESTABLISHED = true; // polaczenia ustanowione
                }
                catch (Exception ie) {
                    ie.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() { // wyswietlenie wiadomosci w watku interfejsu
                    listMsgItems.clear(); // wyczyszczenie historii rozmowy
                    listMsgItems.add(0, String.format("  Server opened! Waiting for clients..."));
                    listAdapter.notifyDataSetChanged();
                }});

            try // czesc odpowiedzialna za odczytywanie wiadomosci
            {
                is = socket.getInputStream();
                os = socket.getOutputStream();
                new Thread(writter).start();

                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(CONTINUE_READ_WRITE) // Odczyt wiadomosci dopoki polaczenie jest otwarte
                {
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0))
                        {
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { // wyswietlenie wiadomosci w watku interfejsu
                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_SHORT).show();
                            listMsgItems.add(0, String.format("< %s", sb.toString())); // wyswietlenie wiadomosci w historii czatu
                            listAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    };

    /**
     * watek klienta
     */
    private Runnable clientConnecter = new Runnable()
    {
        @Override
        public void run()
        {
            try {
                socket = remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                CONNECTION_ESTABLISHED = true; // polaczenie ustanowione

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { // wyswietlenie wiadomosci w watku interfejsu
                        listMsgItems.clear(); // wyczyszczenie historii rozmowy
                        listMsgItems.add(0, String.format("  Ready to communicate! Write something..."));
                        listAdapter.notifyDataSetChanged();
                    }});

                os = socket.getOutputStream();
                is = socket.getInputStream();
                new Thread(writter).start();

                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(CONTINUE_READ_WRITE) // Odczyt wiadomosci dopoki polaczenie jest otwarte
                {
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1)
                    {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0))
                        {
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { // wyswietlenie wiadomosci w watku interfejsu
                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_SHORT).show();
                            listMsgItems.add(0, String.format("< %s", sb.toString()));
                            listAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * watek "pisacza"
     */
    private Runnable writter = new Runnable() {

        @Override
        public void run() {
            while (CONTINUE_READ_WRITE) // odczyt z otwartego strumienia
            {
                try {
                    os.flush();
                    Thread.sleep(2000);
                }
                catch (Exception e) {
                    CONTINUE_READ_WRITE = false;
                }
            }
        }
    };

    /**
     * funkcja wysylania wiadomosci
     * @param v
     */
    public void sendBtnClick(View v)
    {
        if(CONNECTION_ESTABLISHED == false) {
            Toast.makeText(getApplicationContext(), "Connection between devices is not ready.", Toast.LENGTH_SHORT).show();
        }
        else {
            String textToSend = msgET.getText().toString() + " "; // metoda "obcina" ostatni znak, dlatego sztucznie dodajemy spacje
            byte[] b = textToSend.getBytes();
            try {
                os.write(b);
                Toast.makeText(MainActivity.this, msgET.getText().toString(), Toast.LENGTH_SHORT).show();
                listMsgItems.add(0, "> " + msgET.getText().toString()); // wyswietlenie wiadomosci w historii czatu
                listAdapter.notifyDataSetChanged();
                msgET.setText(""); // usuniecie wyslanego tekstu z ET
            }
            catch (IOException e){
                Toast.makeText(getApplicationContext(), "Not sent", Toast.LENGTH_SHORT).show();
            }
        }
    }

}