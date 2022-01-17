package ir.ham3da.darya;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.animation.AnticipateOvershootInterpolator;

import android.widget.ImageView;

import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ir.ham3da.darya.adaptors.BackGroundAdapter;
import ir.ham3da.darya.adaptors.BackGroundItem;
import ir.ham3da.darya.filters.FilterListener;
import ir.ham3da.darya.filters.FilterViewAdapter;
import ir.ham3da.darya.imageeditor.EmojiBSFragment;
import ir.ham3da.darya.imageeditor.PropertiesBSFragment;
import ir.ham3da.darya.imageeditor.ShadowColorDialogFragment;
import ir.ham3da.darya.imageeditor.StickerBSFragment;
import ir.ham3da.darya.imageeditor.TextEditorDialogFragment;
import ir.ham3da.darya.tools.EditingToolsAdapter;
import ir.ham3da.darya.tools.ToolType;
import ir.ham3da.darya.utility.AppFontManager;
import ir.ham3da.darya.utility.AppSettings;
import ir.ham3da.darya.utility.CustomProgress;
import ir.ham3da.darya.utility.SetLanguage;
import ir.ham3da.darya.utility.UtilFunctions;
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;

public class ActivityImageEdit extends AppCompatActivity implements
        OnPhotoEditorListener,
        View.OnClickListener,
        PropertiesBSFragment.Properties,
        EmojiBSFragment.EmojiListener,
        StickerBSFragment.StickerListener,
        EditingToolsAdapter.OnItemSelected,
        FilterListener
{

    String poemText;
    int fontId;

    private static final String TAG = ActivityImageEdit.class.getSimpleName();
    public static final String EXTRA_IMAGE_PATHS = "extra_image_paths";
    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private PropertiesBSFragment mPropertiesBSFragment;
    private EmojiBSFragment mEmojiBSFragment;
    private StickerBSFragment mStickerBSFragment;
    // private TextView mTxtCurrentTool;
    private Typeface mWonderFont;
    private RecyclerView mRvTools, mRvFilters, rvBackground;

    private FilterViewAdapter mFilterViewAdapter = new FilterViewAdapter(this);
    private ConstraintLayout mRootView;
    private ConstraintSet mConstraintSet = new ConstraintSet();
    private boolean mIsFilterVisible, shareRequest, mIsBackgroundVisible;

    private Uri imagSevePath;
    private Typeface mTextIranSansTf;

    List<BackGroundItem> resListBackGroundItem;
    BackGroundAdapter backGroundAdapter;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(SetLanguage.wrap(newBase));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.share_image_menu, menu);
        return true;
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {

        int id = item.getItemId();
        switch (id)
        {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_save:
                saveImage(false);
                break;
            case R.id.action_share:
                shareImage();
                break;


        }
        return super.onOptionsItemSelected(item);
    }

    protected void shareImage()
    {
        if (imagSevePath != null)
        {
            UtilFunctions.shareImage(ActivityImageEdit.this, imagSevePath);
        }
        else
        {
            saveImage(true);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        UtilFunctions.changeTheme(this, true);
        setContentView(R.layout.activity_image_edit);

        AppSettings.Init(this);

        String signature = AppSettings.getSignature();

        EditingToolsAdapter mEditingToolsAdapter = new EditingToolsAdapter(ActivityImageEdit.this, this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.share_as_img);

        poemText = getIntent().getStringExtra("poemText");
        String poetName = getIntent().getStringExtra("poetName");
        PhotoEditorView mPhotoEditorView = findViewById(R.id.photoEditorView);
        fontId = AppSettings.getPoemsFont();
        mTextIranSansTf = AppFontManager.getTypeface(this, fontId);
        initViews();
        mPropertiesBSFragment = new PropertiesBSFragment();
        mEmojiBSFragment = new EmojiBSFragment();
        mStickerBSFragment = new StickerBSFragment();
        mStickerBSFragment.setStickerListener(this);
        mEmojiBSFragment.setEmojiListener(this);
        mPropertiesBSFragment.setPropertiesChangeListener(this);

        LinearLayoutManager llmTools = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvTools.setLayoutManager(llmTools);
        mRvTools.setAdapter(mEditingToolsAdapter);

        LinearLayoutManager llmFilters = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvFilters.setLayoutManager(llmFilters);
        mRvFilters.setAdapter(mFilterViewAdapter);

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                .setDefaultTextTypeface(mTextIranSansTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build(); // build photo editor sdk

        TextStyleBuilder textStyleBuilder = new TextStyleBuilder();
        textStyleBuilder.withTextColor(Color.rgb(0, 0, 0));
        textStyleBuilder.withTextFont(mTextIranSansTf);


        poemText += System.lineSeparator() + poetName;

        if (!signature.isEmpty())
        {
            signature = "«" + signature + "»";
        }

        mPhotoEditor.addText(poemText, textStyleBuilder);
        mPhotoEditor.setOnPhotoEditorListener(this);


        cameraActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bitmap photo = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                        Drawable drawable = new BitmapDrawable(getResources(), photo);
                        mPhotoEditorView.getSource().setImageDrawable(drawable);

                    }
                });

        galleryActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK)
                    {
                        try{
                        Intent data = result.getData();
                        Uri selectedImageURI = data.getData();

                        Bitmap bitmap;

                        if (Build.VERSION.SDK_INT >= 28)
                        {
                            //*** It doesn't work properly and causes an error while saving.***
                            ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), selectedImageURI);
                            bitmap = ImageDecoder.decodeBitmap(source);
                        }
                        else
                        {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageURI);
                        }

                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        mPhotoEditorView.getSource().setImageDrawable(drawable);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    }
                });
    }

    ActivityResultLauncher<Intent> cameraActivityResultLauncher, galleryActivityResultLauncher;


    private void initViews()
    {
        ImageView imgUndo;
        ImageView imgRedo;
        ImageView imgCamera;
        ImageView imgGallery;

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        //mTxtCurrentTool = findViewById(R.id.txtCurrentTool);
        mRvTools = findViewById(R.id.rvConstraintTools);
        mRvFilters = findViewById(R.id.rvFilterView);
        rvBackground = findViewById(R.id.rvBackground);
        mRootView = findViewById(R.id.rootView);

        imgUndo = findViewById(R.id.imgUndo);
        imgUndo.setOnClickListener(this);

        imgRedo = findViewById(R.id.imgRedo);
        imgRedo.setOnClickListener(this);

        imgCamera = findViewById(R.id.imgCamera);
        //imgCamera.setOnClickListener(this);
        imgCamera.setVisibility(View.GONE);

        imgGallery = findViewById(R.id.imgGallery);
        //imgGallery.setOnClickListener(this);
        imgGallery.setVisibility(View.GONE);

        mRvFilters.setVisibility(View.GONE);
        rvBackground.setVisibility(View.GONE);


        resListBackGroundItem = new ArrayList<>();
        for (int i = 1; i <= 15; i++)
        {
            BackGroundItem backGroundItem = new BackGroundItem(this);
            backGroundItem.setId(i);
            resListBackGroundItem.add(backGroundItem);
        }


        backGroundAdapter = new BackGroundAdapter(this, resListBackGroundItem);
        LinearLayoutManager llmBackground = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvBackground.setLayoutManager(llmBackground);
        rvBackground.setAdapter(backGroundAdapter);

        backGroundAdapter.setItemClickListener((position, view) -> {
            int bgID = resListBackGroundItem.get(position).getResIDBig();
            if (view.getId() == R.id.card_view_top )
            {
                mPhotoEditorView.getSource().setImageResource(bgID);
            }
        });

    }


    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height)
    {
        if (drawable instanceof BitmapDrawable)
        {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        return bitmap;
    }


    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode)
    {
        TextEditorDialogFragment textEditorDialogFragment =
                TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener((inputText, colorCode1) -> {
            final TextStyleBuilder styleBuilder = new TextStyleBuilder();
            styleBuilder.withTextColor(colorCode1);
            styleBuilder.withTextFont(mTextIranSansTf);

            mPhotoEditor.editText(rootView, inputText, styleBuilder);
            // mTxtCurrentTool.setText(R.string.label_text);
        });
    }


    @Override
    public void onShadowColorChangeListener(View rootView, int colorCode, float shadowDx, float shadowDy, float shadowRadius)
    {
        ShadowColorDialogFragment shadowColorDialogFragment =
                ShadowColorDialogFragment.show(this, shadowDx, shadowDy, shadowRadius, colorCode);


        shadowColorDialogFragment.setOnShadowColorListener((shadow_Dx, shadow_Dy, shadowRadius1, colorCode1) -> mPhotoEditor.setTextShadow(rootView, shadow_Dx, shadow_Dy, shadowRadius1, colorCode1));
    }


    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews, View rootView)
    {
        Log.d(TAG, "onAddViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews)
    {
        Log.d(TAG, "onRemoveViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType)
    {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType)
    {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {

            case R.id.imgUndo:
                mPhotoEditor.undo();
                break;

            case R.id.imgRedo:
                mPhotoEditor.redo();
                break;

            case R.id.imgCamera:
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraActivityResultLauncher.launch(cameraIntent);

                break;

            case R.id.imgGallery:
                pickFromGallery(); //has problem with api >= 29
                break;
        }
    }






    private void pickFromGallery()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.setAction(Intent.ACTION_GET_CONTENT);

        //startActivityForResult(Intent.createChooser(intent, this.getString(R.string.select_picture)), PICK_REQUEST);
        galleryActivityResultLauncher.launch(Intent.createChooser(intent, this.getString(R.string.select_picture)));

    }

    @SuppressLint("MissingPermission")
    protected void doSaveImage(final boolean share)
    {
        final CustomProgress customProgressDlg = new CustomProgress(this);
        customProgressDlg.showProgress(getString(R.string.saving), getString(R.string.please_wait2), false, false, true);

        try
        {

          //  boolean newFile = file.createNewFile();
            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build();

            String imgFullName = System.currentTimeMillis() + ".jpg";
            mPhotoEditor.saveAsFile(imgFullName, saveSettings, new PhotoEditor.OnSaveListener()
            {
                @Override
                public void onSuccess(@NonNull String imagePath)
                {
                    showSnackbar(getString(R.string.saved));

                    Uri imageUri = Uri.fromFile(new File(imagePath));

                    mPhotoEditorView.getSource().setImageURI(imageUri);
                    customProgressDlg.dismiss();

                    imagSevePath = imageUri;

                    if( android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q )
                    {
                        UtilFunctions.addPicToGallery(getBaseContext(), imagePath);
                    }

                    if (share)
                    {
                        shareImage();
                    }
                }

                @Override
                public void onFailure(@NonNull Exception exception)
                {
                    Log.e(TAG, "saveImage: " + exception.getMessage());
                    exception.printStackTrace();
                    showSnackbar(getString(R.string.failed_save));
                    customProgressDlg.dismiss();
                }
            });
        } catch (Exception e)
        {
            Log.e(TAG, "saveImage: " + e.getMessage());
            showSnackbar(Objects.requireNonNull(e.getMessage()));
            customProgressDlg.dismiss();
        }


    }


    private void saveImage(boolean share)
    {
        shareRequest = share;
        if (isWriteStoragePermissionGranted())
        {
            doSaveImage(share);
        }
    }

    @Override
    public void onColorChanged(int colorCode)
    {
        mPhotoEditor.setBrushColor(colorCode);
    }

    @Override
    public void onOpacityChanged(int opacity)
    {
        mPhotoEditor.setOpacity(opacity);
    }

    @Override
    public void onBrushSizeChanged(int brushSize)
    {
        mPhotoEditor.setBrushSize(brushSize);
    }

    @Override
    public void onEmojiClick(String emojiUnicode)
    {
        mPhotoEditor.addEmoji(emojiUnicode);
    }

    @Override
    public void onStickerClick(Bitmap bitmap)
    {
        mPhotoEditor.addImage(bitmap);
    }


    private void showSaveDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alret_save);

        builder.setPositiveButton(R.string.save, (dialog, which) -> saveImage(false));


        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton(R.string.discard, (dialog, which) -> finish());
        builder.create().show();

    }

    @Override
    public void onFilterSelected(PhotoFilter photoFilter)
    {
        mPhotoEditor.setFilterEffect(photoFilter);
    }

    @Override
    public void onToolSelected(ToolType toolType)
    {
        switch (toolType)
        {
            case BRUSH:
                mPhotoEditor.setBrushDrawingMode(true);
                //mTxtCurrentTool.setText(R.string.label_brush);
                mPropertiesBSFragment.show(getSupportFragmentManager(), mPropertiesBSFragment.getTag());
                break;
            case BACKGROUND:
                showBackgrounds(true);
                break;
            case TEXT:
                TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);

                textEditorDialogFragment.setOnTextEditorListener((inputText, colorCode) -> {
                    final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                    styleBuilder.withTextColor(colorCode);
                    styleBuilder.withTextFont(mTextIranSansTf);

                    mPhotoEditor.addText(inputText, styleBuilder);
                    //mTxtCurrentTool.setText(R.string.label_text);
                });
                break;
            case ERASER:
                mPhotoEditor.brushEraser();
                //  mTxtCurrentTool.setText(R.string.label_eraser);
                break;
            case FILTER:
                // mTxtCurrentTool.setText(R.string.label_filter);
                showFilter(true);
                break;
            case EMOJI:
                mEmojiBSFragment.show(getSupportFragmentManager(), mEmojiBSFragment.getTag());
                break;
            case STICKER:
                mStickerBSFragment.show(getSupportFragmentManager(), mStickerBSFragment.getTag());
                break;

        }
    }


    protected void showBackgrounds(boolean isVisible)
    {
        mIsBackgroundVisible = isVisible;
        mConstraintSet.clone(mRootView);
        Log.e(TAG, "showBackgrounds: " + isVisible);
        if (isVisible)
        {
            rvBackground.setVisibility(View.VISIBLE);
        }
        else
        {
            rvBackground.setVisibility(View.GONE);

        }

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(350);
        changeBounds.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
        TransitionManager.beginDelayedTransition(mRootView, changeBounds);

        mConstraintSet.applyTo(mRootView);
    }

    protected void showFilter(boolean isVisible)
    {
        mIsFilterVisible = isVisible;
        mConstraintSet.clone(mRootView);
        Log.e(TAG, "showFilter: " + isVisible);
        if (isVisible)
        {

            mRvFilters.setVisibility(View.VISIBLE);

        }
        else
        {
            mRvFilters.setVisibility(View.GONE);

        }

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(350);
        changeBounds.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
        TransitionManager.beginDelayedTransition(mRootView, changeBounds);

        mConstraintSet.applyTo(mRootView);
    }

    @Override
    public void onBackPressed()
    {
        if (mIsFilterVisible || mIsBackgroundVisible)
        {
            if(mIsFilterVisible)
            {
                showFilter(false);
            }
            if(mIsBackgroundVisible)
            {
                showBackgrounds(false);
            }
        }
        else if (!mPhotoEditor.isCacheEmpty())
        {
            showSaveDialog();
        }
        else
        {
            super.onBackPressed();
            Bungee.slideDown(this); //fire the slide left animation
        }
    }


    //Permission

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case 2:
                Log.d(TAG, "Write External storage");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.v(TAG, "Permission: " + permissions[0] + " was " + grantResults[0]);
                    //resume tasks needing this permission
                    saveImage(shareRequest);
                }

                break;

            case 3:
                Log.d(TAG, "Read External storage");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.v(TAG, "Permission: " + permissions[0] + " was " + grantResults[0]);
                    //resume tasks needing this permission
                    //SharePdfFile();
                }

                break;
        }
    }

    public boolean isReadStoragePermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
            {
                Log.v(TAG, "Permission is granted1");
                return true;
            }
            else
            {

                Log.v(TAG, "Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else
        { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted1");
            return true;
        }
    }

    public boolean isWriteStoragePermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
            {
                Log.v(TAG, "Permission is granted2");
                return true;
            }
            else
            {

                Log.v(TAG, "Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else
        { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted2");
            return true;
        }
    }


    protected void showSnackbar(@NonNull String message)
    {
        View view = findViewById(android.R.id.content);
        if (view != null)
        {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }


}
