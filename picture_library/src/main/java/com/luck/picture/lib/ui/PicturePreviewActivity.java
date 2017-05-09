package com.luck.picture.lib.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.picture.lib.R;
import com.luck.picture.lib.compress.CompressConfig;
import com.luck.picture.lib.compress.CompressImageOptions;
import com.luck.picture.lib.compress.CompressInterface;
import com.luck.picture.lib.compress.LubanOptions;
import com.luck.picture.lib.model.FunctionConfig;
import com.luck.picture.lib.model.PictureConfig;
import com.luck.picture.lib.observable.ImagesObservable;
import com.luck.picture.lib.widget.PreviewViewPager;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.dialog.OptAnimationLoader;
import com.yalantis.ucrop.dialog.SweetAlertDialog;
import com.yalantis.ucrop.entity.EventEntity;
import com.yalantis.ucrop.entity.LocalMedia;
import com.yalantis.ucrop.util.ToolbarUtil;
import com.yalantis.ucrop.util.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.luck.picture.lib.model.FunctionConfig.CLOSE_SINE_CROP_FLAG;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 */
public class PicturePreviewActivity extends PictureBaseActivity implements View.OnClickListener, Animation.AnimationListener {
    private ImageView picture_left_back;
    private TextView tv_img_num, tv_title, tv_ok, tv_edit;
    private RelativeLayout select_bar_layout;
    private PreviewViewPager viewPager;
    private int position;

    private RelativeLayout rl_title;
    private LinearLayout ll_check;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();
    private TextView check;
    private SimpleFragmentAdapter adapter;
    private Animation animation;
    private boolean refresh;
    private SweetAlertDialog dialog;

    //EventBus 3.0 回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventBus(EventEntity obj) {
        switch (obj.what) {
            case FunctionConfig.CLOSE_FLAG:
                dismiss();
                closeActivity();
                break;
            case FunctionConfig.CLOSE_PREVIEW_FLAG:
                closeActivity();
                break;
            case FunctionConfig.CROP_FLAG:
                // 裁剪返回的数据
                List<LocalMedia> result = obj.medias;
                if (result == null)
                    result = new ArrayList<>();
                handleCropResult(result);
                break;
        }
    }

    private void handleCropResult(List<LocalMedia> result) {
        if (result != null) {
            if (type == FunctionConfig.TYPE_IMAGE) {
                onSelectDone(result);
            }
        }
    }

    public void onSelectDone(List<LocalMedia> newResult) {

        int selectedPosition = viewPager.getCurrentItem();
        for (LocalMedia media : newResult) {

            if (selectedPosition < images.size()){
                LocalMedia srcMedia = images.get(selectedPosition);
                media.setPath(media.getCutPath());

                for (int i = 0; i < selectImages.size(); i++){
                    if (srcMedia.getPath().equals(selectImages.get(i).getPath())){
                        selectImages.set(i,media);
                        break;
                    }
                }

                for (int i = 0; i < images.size(); i++){
                    if (srcMedia.getPath().equals(images.get(i).getPath())){
                        images.set(i,media);
                        if (adapter != null){
                            adapter.replacePicture(i,images.get(i).getPath());
                        }
                        updateSelector(true);
                        break;
                    }
                }

            }

            break;
        }

        EventEntity obj = new EventEntity(CLOSE_SINE_CROP_FLAG);
        EventBus.getDefault().post(obj);


    }

    /**
     * 处理图片压缩
     */
    private void compressImage(final List<LocalMedia> result) {
        showPleaseDialog(getString(R.string.picture_please));
        CompressConfig compress_config = CompressConfig.ofDefaultConfig();
        switch (compressFlag) {
            case 1:
                // 系统自带压缩
                compress_config.enablePixelCompress(options.isEnablePixelCompress());
                compress_config.enableQualityCompress(options.isEnableQualityCompress());
                compress_config.setMaxSize(maxB);
                break;
            case 2:
                // luban压缩
                LubanOptions option = new LubanOptions.Builder()
                        .setMaxHeight(compressH)
                        .setMaxWidth(compressW)
                        .setMaxSize(maxB)
                        .setGrade(grade)
                        .create();
                compress_config = CompressConfig.ofLuban(option);
                break;
        }

        CompressImageOptions.compress(this, compress_config, result, new CompressInterface.CompressListener() {
            @Override
            public void onCompressSuccess(List<LocalMedia> images) {
                // 压缩成功回调
                onResult(images,true);
                dismiss();
            }

            @Override
            public void onCompressError(List<LocalMedia> images, String msg) {
                // 压缩失败回调 返回原图
                onResult(result,true);
                dismiss();
            }
        }).compress();
    }

    // 关闭activity
    protected void closeActivity() {
        finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_activity_image_preview);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        rl_title = (RelativeLayout) findViewById(R.id.rl_title);
        picture_left_back = (ImageView) findViewById(R.id.picture_left_back);
        viewPager = (PreviewViewPager) findViewById(R.id.preview_pager);
        ll_check = (LinearLayout) findViewById(R.id.ll_check);
        select_bar_layout = (RelativeLayout) findViewById(R.id.select_bar_layout);
        check = (TextView) findViewById(R.id.check);
        picture_left_back.setOnClickListener(this);
        tv_ok = (TextView) findViewById(R.id.tv_ok);
        tv_edit = (TextView) findViewById(R.id.tv_edit);
        tv_img_num = (TextView) findViewById(R.id.tv_img_num);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_ok.setOnClickListener(this);
        tv_edit.setOnClickListener(this);
        position = getIntent().getIntExtra(FunctionConfig.EXTRA_POSITION, 0);
        tv_ok.setTextColor(completeColor);
        tv_edit.setTextColor(completeColor);
        select_bar_layout.setBackgroundColor(previewBottomBgColor);
        rl_title.setBackgroundColor(previewTopBgColor);
        ToolbarUtil.setColorNoTranslucent(this, previewTopBgColor);
        animation = OptAnimationLoader.loadAnimation(this, R.anim.modal_in);
        animation.setAnimationListener(this);
        boolean is_bottom_preview = getIntent().getBooleanExtra(FunctionConfig.EXTRA_BOTTOM_PREVIEW, false);
        if (is_bottom_preview) {
            // 底部预览按钮过来
            images = (List<LocalMedia>) getIntent().getSerializableExtra(FunctionConfig.EXTRA_PREVIEW_LIST);
        } else {
            images = ImagesObservable.getInstance().readLocalMedias();
        }

        if (is_checked_num) {
            tv_img_num.setBackgroundResource(cb_drawable);
            tv_img_num.setSelected(true);
        }

        selectImages = (List<LocalMedia>) getIntent().getSerializableExtra(FunctionConfig.EXTRA_PREVIEW_SELECT_LIST);

        initViewPageAdapterData();
        ll_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 刷新图片列表中图片状态
                boolean isChecked;
                if (!check.isSelected()) {
                    isChecked = true;
                    check.setSelected(true);
                    check.startAnimation(animation);
                } else {
                    isChecked = false;
                    check.setSelected(false);
                }
                if (selectImages.size() >= maxSelectNum && isChecked) {
                    Toast.makeText(PicturePreviewActivity.this, getString(R.string.picture_message_max_num, maxSelectNum), Toast.LENGTH_LONG).show();
                    check.setSelected(false);
                    return;
                }
                LocalMedia image = images.get(viewPager.getCurrentItem());
                if (isChecked) {
                    selectImages.add(image);
                    image.setNum(selectImages.size());
                    if (is_checked_num) {
                        check.setText(image.getNum() + "");
                    }
                } else {
                    for (LocalMedia media : selectImages) {
                        if (media.getPath().equals(image.getPath())) {
                            selectImages.remove(media);
                            subSelectPosition();
                            notifyCheckChanged(media);
                            break;
                        }
                    }
                }
                onSelectNumChange(true);
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                tv_title.setText(position + 1 + "/" + images.size());
                if (is_checked_num) {
                    LocalMedia media = images.get(position);
                    check.setText(media.getNum() + "");
                    notifyCheckChanged(media);
                }
                onImageChecked(position);


            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void initViewPageAdapterData() {
        tv_title.setText(position + 1 + "/" + images.size());
        adapter = new SimpleFragmentAdapter(getSupportFragmentManager());
        check.setBackgroundResource(cb_drawable);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        onSelectNumChange(false);
        onImageChecked(position);
        if (is_checked_num) {
            tv_img_num.setBackgroundResource(cb_drawable);
            tv_img_num.setSelected(true);
            LocalMedia media = images.get(position);
            check.setText(media.getNum() + "");
            notifyCheckChanged(media);
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(LocalMedia imageBean) {
        if (is_checked_num) {
            check.setText("");
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(imageBean.getPath())) {
                    imageBean.setNum(media.getNum());
                    check.setText(String.valueOf(imageBean.getNum()));
                }
            }
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && position < images.size()) {
            check.setSelected(isSelected(images.get(position)));
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */

    public void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = selectImages.size() != 0;
        if (enable) {
            tv_ok.setEnabled(true);
            tv_img_num.setVisibility(View.VISIBLE);
            tv_img_num.startAnimation(animation);
            tv_img_num.setText(selectImages.size() + "");
            tv_ok.setText(getString(R.string.picture_completed));
        } else {
            tv_ok.setEnabled(false);
            tv_img_num.setVisibility(View.INVISIBLE);
            tv_ok.setText(getString(R.string.picture_please_select));
            updateSelector(refresh);
        }
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    private void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            EventEntity obj = new EventEntity(FunctionConfig.UPDATE_FLAG, selectImages);
            EventBus.getDefault().post(obj);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        updateSelector(refresh);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }


    public class SimpleFragmentAdapter extends FragmentPagerAdapter {

        private PictureImagePreviewFragment[] mFragments ;

        public SimpleFragmentAdapter(FragmentManager fm) {
            super(fm);
            mFragments = new PictureImagePreviewFragment[images.size()];
        }

        @Override
        public Fragment getItem(int position) {
            PictureImagePreviewFragment fragment = PictureImagePreviewFragment.getInstance(images.get(position).getPath(), selectImages);
            mFragments[position] = fragment;
            return mFragments[position];
        }

        public void replacePicture(int position, String path){
            if (path != null && position < mFragments.length){
                mFragments[position].replacePicture(path,selectImages);
            }
        }

        @Override
        public int getCount() {
            return images.size();
        }


    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.picture_left_back) {
            finish();
        } else if (id == R.id.tv_ok) {
            // 如果设置了图片最小选择数量，则判断是否满足条件
            int size = selectImages.size();
            if (minSelectNum > 0) {
                if (size < minSelectNum && selectMode == FunctionConfig.MODE_MULTIPLE) {
                    switch (type) {
                        case FunctionConfig.TYPE_IMAGE:
                            showToast(getString(R.string.picture_min_img_num, options.getMinSelectNum()));
                            return;
                        case FunctionConfig.TYPE_VIDEO:
                            showToast(getString(R.string.picture_min_video_num, options.getMinSelectNum()));
                            return;
                        default:
                            break;
                    }
                }
            }

            if (isCompress && type == FunctionConfig.TYPE_IMAGE) {
                // 压缩图片
                compressImage(selectImages);
            } else {
                onResult(selectImages, false);
            }

        }else if (id == R.id.tv_edit){
            int selectedPosition = viewPager.getCurrentItem();
            if (images != null && selectedPosition < images.size()) {
                startCopy(images.get(selectedPosition).getPath());
            }
        }
    }

    public void onResult(List<LocalMedia> images, boolean isCompressed) {
        // 因为这里是单一实例的结果集，重新用变量接收一下在返回，不然会产生结果集被单一实例清空的问题
        List<LocalMedia> result = new ArrayList<>();
        for (LocalMedia media : images) {
            result.add(media);
        }
        PictureConfig.OnSelectResultCallback resultCallback = PictureConfig.getInstance().getResultCallback();
        if (resultCallback != null) {
            resultCallback.onSelectSuccess(result);
        }

        //如果压缩完成后，则退出
        if (isCompressed){
            EventEntity obj = new EventEntity(FunctionConfig.CLOSE_FLAG, result);
            EventBus.getDefault().post(obj);
            overridePendingTransition(0, R.anim.slide_bottom_out);
            return;
        }
        // 如果开启了压缩，先不关闭此页面，PictureImageGridActivity压缩完在通知关闭
        if (!isCompress) {
            EventEntity obj = new EventEntity(FunctionConfig.CLOSE_FLAG, result);
            EventBus.getDefault().post(obj);
            overridePendingTransition(0, R.anim.slide_bottom_out);
        } else {
            showPleaseDialog(getString(R.string.picture_please));
        }
    }
    /**
     * 裁剪
     *
     * @param path
     */
    protected void startCopy(String path) {
        // 如果开启裁剪 并且是单选
        // 去裁剪
        if (Utils.isFastDoubleClick()) {
            return;
        }
        UCrop uCrop = UCrop.of(Uri.parse(path), Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis() + ".jpg")));
        UCrop.Options options = new UCrop.Options();
        switch (copyMode) {
            case FunctionConfig.CROP_MODEL_DEFAULT:
                options.withAspectRatio(0, 0);
                break;
            case FunctionConfig.CROP_MODEL_1_1:
                options.withAspectRatio(1, 1);
                break;
            case FunctionConfig.CROP_MODEL_3_2:
                options.withAspectRatio(3, 2);
                break;
            case FunctionConfig.CROP_MODEL_3_4:
                options.withAspectRatio(3, 4);
                break;
            case FunctionConfig.CROP_MODEL_16_9:
                options.withAspectRatio(16, 9);
                break;
        }

        // 圆形裁剪
        if (circularCut) {
            options.setCircleDimmedLayer(true);// 是否为椭圆
            options.setShowCropFrame(false);// 外部矩形
            options.setShowCropGrid(false);// 内部网格
            options.withAspectRatio(1, 1);
        }
        options.setCompressionQuality(compressQuality);
        options.withMaxResultSize(cropW, cropH);
        options.background_color(backgroundColor);
        options.localType(type);
        options.setIsCompress(isCompress);
        options.setCircularCut(circularCut);
        uCrop.withOptions(options);
        uCrop.start(PicturePreviewActivity.this);
    }


    private void showPleaseDialog(String msg) {
        if (!isFinishing()) {
            dialog = new SweetAlertDialog(PicturePreviewActivity.this);
            dialog.setTitleText(msg);
            dialog.show();
        }
    }

    private void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }
}
