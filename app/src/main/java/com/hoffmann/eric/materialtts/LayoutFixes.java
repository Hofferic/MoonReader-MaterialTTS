package com.hoffmann.eric.materialtts;

import android.content.Context;
import android.content.res.XModuleResources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.SoftReference;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

/**
 * Copyright Eric Hoffmann, created on 07.12.2015.
 */
public class LayoutFixes implements IXposedHookInitPackageResources, IXposedHookZygoteInit {
    private static final String PACKAGE_PREFIX = "com.flyersoft.moonreader";
    private static String MODULE_PATH = null;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(final XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        if(!resParam.packageName.startsWith(PACKAGE_PREFIX)){
            return;
        }

        XposedBridge.log("MoonReader res hook, package: " + resParam.packageName);


        final XModuleResources res = XModuleResources.createInstance(MODULE_PATH, resParam.res);

        ////////// general tts stuff /////////////

        // replace control icons
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_page_up", res.fwd(R.drawable.ic_chevron_double_left_grey600_24dp));
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_prior", res.fwd(R.drawable.ic_chevron_left_grey600_36dp));
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_pause", res.fwd(R.drawable.ic_pause_grey600_36dp));
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_next", res.fwd(R.drawable.ic_chevron_right_grey600_36dp));
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_page_down", res.fwd(R.drawable.ic_chevron_double_right_grey600_24dp));


        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_play", res.fwd(R.drawable.ic_play_grey600_36dp));
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_stop", res.fwd(R.drawable.ic_stop_grey600_36dp));
        resParam.res.setReplacement(resParam.packageName, "drawable", "tts_options", res.fwd(R.drawable.ic_settings_grey600_36dp));

        // replace floating tts settings icon
        resParam.res.setReplacement(resParam.packageName, "drawable", "ttsb1", res.fwd(R.drawable.ic_text_to_speech_grey600_48dp_90a));
        resParam.res.setReplacement(resParam.packageName, "drawable", "ttsb2", res.fwd(R.drawable.ic_text_to_speech_grey600_48dp));

        // replace status bar icon for tts
        resParam.res.setReplacement(resParam.packageName, "drawable", "ttsb3", res.fwd(R.drawable.ic_text_to_speech_white_notify));

        // cheat by replacing unused tts drawable for use in notification
        resParam.res.setReplacement(resParam.packageName, "drawable", "ic_launcher", res.fwd(R.drawable.ic_play_pause_grey600_36dp));

        //////// tts notification /////////

        resParam.res.hookLayout(resParam.packageName, "layout", "tts_notification", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                XposedBridge.log("MoonReader tts_notification hook");
                FrameLayout frame = (FrameLayout) liparam.view;
                float density = frame.getContext().getResources().getDisplayMetrics().density;

                // fix notification background
                frame.setBackground(null);

                // replace TTS notification icon

                ImageView icon = new ImageView(frame.getContext());
                icon.setImageResource(
                        resParam.res.getIdentifier("ttsb2", "drawable", resParam.packageName)
                );
                frame.removeViewAt(0);
                frame.addView(icon, 0);

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) icon.getLayoutParams();
                params.gravity = Gravity.CENTER_VERTICAL;
                params.width = (int)(40 * density);
                params.height = FrameLayout.LayoutParams.MATCH_PARENT;
                params.leftMargin = (int)(5 * density);
                icon.requestLayout();

                // fix close icon
                ImageView close = ((ImageView)frame.getChildAt(frame.getChildCount()-1));
                close.setImageResource(
                        resParam.res.getIdentifier("dialog_close", "drawable", resParam.packageName)
                );

                // remove ugly blue "active" background
                close.setBackground(null);
                LinearLayout buttons = (LinearLayout) frame.getChildAt(1);
                for(int i = buttons.getChildCount()-1; i>=0; i--){
                    buttons.getChildAt(i).setBackground(null);
                }


                // replace play icon with play/pause icon as it is not updated upon status change by moon reader

                ((ImageView) buttons.getChildAt(2)).setImageResource(
                        resParam.res.getIdentifier("ic_launcher", "drawable", resParam.packageName)
                );

                /*final int playRes = resParam.res.getIdentifier("tts_play", "drawable", resParam.packageName);
                final int pauseRes = resParam.res.getIdentifier("tts_pause", "drawable", resParam.packageName);

                final ImageViewDelegate button = new ImageViewDelegate((ImageView) buttons.getChildAt(2));
                button.setImageResource(pauseRes);
                button.setAdditinalOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        LayoutFixes.TTS_PAUSED = !LayoutFixes.TTS_PAUSED;
                        XposedBridge.log("tts state: " + (LayoutFixes.TTS_PAUSED ? "TTS_PAUSED" : "playing"));
                        button.setImageResource(LayoutFixes.TTS_PAUSED ? playRes : pauseRes);
                    }
                });

                buttons.removeViewAt(2);
                buttons.addView(button, 2);*/
            }
        });


        ///////// tts settings panel /////////

        resParam.res.hookLayout(resParam.packageName, "layout", "tts_panel", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                XposedBridge.log("MoonReader tts_panel hook");
                LinearLayout parent = (LinearLayout) liparam.view;
                LinearLayout buttons = (LinearLayout) parent.getChildAt(1);
                LinearLayout seekbars = (LinearLayout) parent.getChildAt(0);
                float density = parent.getContext().getResources().getDisplayMetrics().density;

                /////////// remove tacky gradient /////////
                parent.setBackground(new ColorDrawable(0xff333333));


                /////// remove tacky glowing effect for pressed buttons and add color filter /////////

                ColorFilter grey400 = new PorterDuffColorFilter(0xffbdbdbd, PorterDuff.Mode.SRC_IN);
                ImageView tempImage;

                // main control buttons
                LinearLayout buttonViews = (LinearLayout) ((FrameLayout) buttons.getChildAt(1)).getChildAt(0);
                for(int i = buttonViews.getChildCount()-1; i>=0; i--){
                    tempImage = (ImageView)buttonViews.getChildAt(i);
                    tempImage.setBackground(null);
                    tempImage.setColorFilter(grey400);
                }

                // set play/pause as master button to notification
                /*ImageViewDelegate master = new ImageViewDelegate((ImageView) buttonViews.getChildAt(2));
                buttonViews.removeViewAt(2);
                buttonViews.addView(master, 2);
                master.setAdditionalOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        XposedBridge.log("master button clicked, paused: " + LayoutFixes.TTS_PAUSED);
                    }
                });*/

                // stop
                tempImage = (ImageView) buttons.getChildAt(0);
                tempImage.setBackground(null);
                tempImage.getDrawable().mutate();
                tempImage.setColorFilter(grey400);

                // options
                tempImage = (ImageView) buttons.getChildAt(2);
                tempImage.setBackground(null);
                tempImage.getDrawable().mutate();
                tempImage.setColorFilter(grey400);


                //////// use system default seekbar style + moon reader thumb and change label colors ///////

                LinearLayout left = (LinearLayout) seekbars.getChildAt(0);
                LinearLayout center = (LinearLayout) seekbars.getChildAt(1);
                LinearLayout right = (LinearLayout) seekbars.getChildAt(2);

                SeekBar seekVolume = (SeekBar) left.getChildAt(1);
                SeekBar seekPitch = (SeekBar) center.getChildAt(1);
                SeekBar seekSpeed = (SeekBar) right.getChildAt(1);

                // the easiest way to get instantiated default drawables is to let the
                // system do it. Also the different seekbars MUST not share the
                // same instance as that would cause changes to carry over
                // between seekbars
                SeekBar tempSeekBar = new SeekBar(seekbars.getContext());
                seekVolume.setProgressDrawable(tempSeekBar.getProgressDrawable());
                seekVolume.setThumb(
                        parent.getContext().getResources().getDrawable(
                                resParam.res.getIdentifier("scrubber_control_selector_holo3", "drawable", resParam.packageName),
                                null
                        ));


                tempSeekBar = new SeekBar(seekbars.getContext());
                seekPitch.setProgressDrawable(tempSeekBar.getProgressDrawable());
                seekPitch.setThumb(
                        parent.getContext().getResources().getDrawable(
                                resParam.res.getIdentifier("scrubber_control_selector_holo3", "drawable", resParam.packageName),
                                null
                        ));

                tempSeekBar = new SeekBar(seekbars.getContext());
                seekSpeed.setProgressDrawable(tempSeekBar.getProgressDrawable());
                seekSpeed.setThumb(
                        parent.getContext().getResources().getDrawable(
                                resParam.res.getIdentifier("scrubber_control_selector_holo3", "drawable", resParam.packageName),
                                null
                        ));

                // and now for the label colors
                TextView volume = (TextView) left.getChildAt(0);
                TextView pitch = (TextView) center.getChildAt(0);

                LinearLayout meta = (LinearLayout) right.getChildAt(0);
                TextView speed = (TextView) meta.getChildAt(0);
                TextView reset = (TextView) meta.getChildAt(1);

                volume.setTextColor(0xb3ffffff);
                pitch.setTextColor(0xb3ffffff);
                speed.setTextColor(0xb3ffffff);

                reset.setTextColor(0xff1565c0);
                // also, the tacky blue active background
                reset.setBackground(null);

                ////////// change volume seeker values //////
                seekVolume.setMax(
                        ((AudioManager)parent.getContext().getSystemService(Context.AUDIO_SERVICE))
                                .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                );


                ///////// rearrange seekbars to make more room for speed /////////
                LinearLayout volPitch = new LinearLayout(seekbars.getContext());
                volPitch.setOrientation(LinearLayout.HORIZONTAL);

                seekbars.removeView(left);
                seekbars.removeView(center);

                volPitch.addView(left);
                volPitch.addView(center);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) left.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                params.weight = 1.0f;
                params = (LinearLayout.LayoutParams) center.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                params.weight = 1.0f;

                seekbars.addView(volPitch, seekbars.getChildCount());
                seekbars.setOrientation(LinearLayout.VERTICAL);

                params = (LinearLayout.LayoutParams) volPitch.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                params.bottomMargin = (int) (6 * density);

                seekbars.requestLayout();


                ///////// swap buttons to top and add dividing line //////////

                parent.removeView(buttons);
                parent.addView(buttons, 0);

                // upper divider
                View divider = new View(parent.getContext());
                divider.setBackground(new ColorDrawable(0xff555555));
                parent.addView(divider, 0);
                params = (LinearLayout.LayoutParams) divider.getLayoutParams();
                params.height = (int) density;
                params.topMargin = 0;
                params.bottomMargin = (int) density;
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;

                // remove top padding from parent
                parent.setPadding(
                        parent.getPaddingLeft(),
                        0,
                        parent.getPaddingRight(),
                        parent.getPaddingBottom()
                );

                // lower divider
                divider = new View(parent.getContext());
                divider.setBackground(new ColorDrawable(0xff555555));
                parent.addView(divider, 2);
                params = (LinearLayout.LayoutParams) divider.getLayoutParams();
                params.height = (int) density;
                params.topMargin = (int) (2*density);
                params.bottomMargin = (int) (4*density);
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;

                parent.requestLayout();
            }
        });
    }

    /*private class ImageViewDelegate extends ImageView{
        private View.OnClickListener dynamicListener;
        private View.OnClickListener additinalListener;
        private View.OnClickListener delegatingListener;

        public ImageViewDelegate(ImageView original){
            super(original.getContext());
            this.setImageDrawable(original.getDrawable());
            this.setLayoutParams(original.getLayoutParams());
            this.setId(original.getId());
            this.setColorFilter(original.getColorFilter());

            delegatingListener = new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    XposedBridge.log("ImageViewDelegate clicked");
                    if(dynamicListener!=null){
                        dynamicListener.onClick(v);
                    }
                    if(additinalListener != null) {
                        additinalListener.onClick(v);
                    }
                }
            };

            super.setOnClickListener(delegatingListener);
        }

        @Override
        public void setOnClickListener(View.OnClickListener listener){
            XposedBridge.log("ImageViewDelegate OnClickListener set " + listener.toString());
            dynamicListener = listener;
        }

        public void setAdditionalOnClickListener(View.OnClickListener listener){
            additinalListener = listener;
        }
    }*/
}
