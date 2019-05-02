/*
 * Copyright (C) 2016 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.folioreader.android.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.ui.base.OnSaveHighlight;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;
import lib.folderpicker.FolderPicker;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import android.util.DisplayMetrics; // Needed for aspect ratio and res support fix

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener {

    private static final String LOG_TAG = HomeActivity.class.getSimpleName();
    private FolioReader folioReader;
    public int pastButtons = 0;
    public static String usrFolder = "/storage/emulated/0/Download/";
    private static final int REQ_PERMISSION = 120,
            FOLDERPICKER_CODE = 2,
            RESULT_CODE = 1,
            FILE_PICKER_CODE = 3;

    /** Called when the activity is first created. */
    private TextView fileLoc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);

        getHighlightsAndSave();

        debugButtons();

        fileLoc = findViewById(R.id.file_location);
        fileLoc.setText(usrFolder);

        findViewById(R.id.file_explorer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pickFolder();

                usrFolder = fileLoc.getText().toString();

            }
        });
        findViewById(R.id.update).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usrFolder = fileLoc.getText().toString();
                File rootFolder = new File(usrFolder);
                genButtons(folderScan(rootFolder), rootFolder);

            }
        });
    }

    void debugButtons(){


        findViewById(R.id.btn_assest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ReadLocator readLocator = getLastReadLocator();

                Config config = AppUtil.getSavedConfig(getApplicationContext());
                if (config == null)
                    config = new Config();
                config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);

                folioReader.setReadLocator(readLocator);
                folioReader.setConfig(config, true)
                        .openBook("file:///android_asset/TheSilverChair.epub\"");

            }
        });

    }

    void genButtons(int bookCount, File folder){
        RelativeLayout layout = findViewById(R.id.activity_home);


        for(int i=0; i< pastButtons;i++)
        {
            Button btn;
            btn = findViewById(i);
            layout.removeView(btn);
        }

        FileFilter fileFilter = new WildcardFileFilter("*.epub");
        final File[] listOfFiles = folder.listFiles(fileFilter);
        int btnCount = 0;


        DisplayMetrics displayMetrics = new DisplayMetrics();  // adds support for alternative aspect ratios eg 18.5:9
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        //int width = displayMetrics.widthPixels;
        int yIdent = (int) (height / 3.55) ; //tested values 2 - 4 ; // Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2  ;  // 800 for 16:9 TM... adds support for alternative aspect ratios eg 18.5:9
        //int yIdent = 800;
        int i;
        for (i = 0; i <bookCount; i++) {
            final String btnName;
            String bookName = listOfFiles[i].getName();
            bookName = bookName.replaceAll(".epub","");
            final String bookPath = ""+listOfFiles[i];
            btnName = bookName;
            Button btnTag = new Button(this);
            btnTag.setText(btnName);
            btnTag.setId(btnCount);
            btnTag.setTag(btnName);
            btnTag.setY(yIdent);
            btnTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReadLocator readLocator = getLastReadLocator();
                    Config config = AppUtil.getSavedConfig(getApplicationContext());
                    if (config == null)
                        config = new Config();
                    config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
                    folioReader.setReadLocator(readLocator);
                    folioReader.setConfig(config, true).openBook(bookPath);
                }
            });

            layout.addView(btnTag);
            btnCount++;
                //yIdent += 150;

                yIdent += 150 * height / 1920; // gap between epub buttons ... makes gaps more consistent across alternate resolutions and aspect ratios etc

        }

        pastButtons = btnCount;

    }

    public void pickFolder() {
        Intent intent = new Intent(this, FolderPicker.class);
        startActivityForResult(intent, FOLDERPICKER_CODE);
        onActivityResult(FOLDERPICKER_CODE, RESULT_CODE, intent);
        Log.d("folderLocation", usrFolder);
    }



    public int folderScan(File folder) {

        FileFilter fileFilter = new WildcardFileFilter("*.epub");
        File[] listOfFiles = folder.listFiles(fileFilter);

        System.out.println(listOfFiles.length);
        int totalFiles = 0;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                totalFiles++;
                System.out.println("File " + listOfFiles[i].getName());
            }
        }
        return totalFiles;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {

            String folderLocation = intent.getExtras().getString("data");
            Log.i( "folderLocation", folderLocation );
            fileLoc.setText(folderLocation);


        }
    }

    private ReadLocator getLastReadLocator() {

        String jsonString = loadAssetTextAsString("Locators/LastReadLocators/last_read_locator_1.json");
        return ReadLocator.fromJson(jsonString);
    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson());
    }

    /*
     * For testing purpose, we are getting dummy highlights from asset. But you can get highlights from your server
     * On success, you can save highlights to FolioReader DB.
     */
    private void getHighlightsAndSave() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HighLight> highlightList = null;
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    highlightList = objectMapper.readValue(
                            loadAssetTextAsString("highlights/highlights_data.json"),
                            new TypeReference<List<HighlightData>>() {
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (highlightList == null) {
                    folioReader.saveReceivedHighLights(highlightList, new OnSaveHighlight() {
                        @Override
                        public void onFinished() {
                            //You can do anything on successful saving highlight list
                        }
                    });
                }
            }
        }).start();
    }

    private String loadAssetTextAsString(String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ((str = in.readLine()) != null) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e("HomeActivity", "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "Error closing asset " + name);
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FolioReader.clear();
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
        Toast.makeText(this,
                "highlight id = " + highlight.getUUID() + " type = " + type,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed");
    }
}