package com.futurice.android.reservator.view;

import com.futurice.android.reservator.R;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.test.suitebuilder.annotation.Suppress;
import android.text.Html;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

import com.futurice.android.reservator.common.Helpers;
import com.futurice.android.reservator.model.Room;
import com.futurice.android.reservator.model.Reservation;
import com.futurice.android.reservator.model.TimeSpan;

public class RoomTrafficLights extends RelativeLayout {
    private static Date lastTimeConnected = new Date(0);
    final long TOUCH_TIMEOUT = 2 * 1000; // was 30
    final long TOUCH_TIMER = 1 * 1000; // was 10
    final int QUICK_BOOK_THRESHOLD = 5; // minutes
    // Show "disconnected" warning icon on screen when disconnected for more than 5 minutes
    private final long DISCONNECTED_WARNING_ICON_THRESHOLD = 5 * 60 * 1000;
    TextView roomStatusView;
    TextView roomTitleView;
    TextView roomStatusInfoView;
    TextView reservationInfoView;
    Button bookNowView;
    View disconnected;
    Timer touchTimeoutTimer;
    boolean enabled = true;
    View.OnClickListener bookNowListener;
    private long lastTouched = 0;

    public RoomTrafficLights(Context context) {
        this(context, null);
    }

    public RoomTrafficLights(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.room_traffic_lights, this);
        roomTitleView = (TextView) findViewById(R.id.roomTitle);
        roomStatusView = (TextView) findViewById(R.id.roomStatus);
        roomStatusInfoView = (TextView) findViewById(R.id.roomStatusInfo);
        reservationInfoView = (TextView) findViewById(R.id.reservationInfo);
        bookNowView = (Button) findViewById(R.id.bookNow);
        disconnected = findViewById(R.id.disconnected);
        updateConnected();

        setClickable(true);
        setVisibility(INVISIBLE);

        bookNowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bookNowListener != null && bookNowView.getVisibility() == VISIBLE) {
                    bookNowListener.onClick(v);
                }
            }
        });
    }

    public void setBookNowListener(View.OnClickListener l) {
        this.bookNowListener = l;
    }

    public void update(Room room) {
        updateConnected();

        roomTitleView.setText(room.getName());

        if (room.isBookable(QUICK_BOOK_THRESHOLD)) {
            roomStatusView.setText(getContext().getString(R.string.free));
            if (room.isFreeRestOfDay()) {
                roomStatusInfoView.setText(getContext().getString(R.string.forTheDay));
                this.setBackgroundColor(getResources().getColor(R.color.TrafficLightFree));
                // Must use deprecated API for some reason or it crashes on older tablets
                bookNowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.traffic_lights_button_green));
                bookNowView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_green));
            } else {
                int freeMinutes = room.minutesFreeFromNow();
                roomStatusView.setText(getContext().getString(R.string.free));
                roomStatusInfoView.setText(getContext().getString(R.string.forTimespan) + " " + Helpers.humanizeTimeSpan2(freeMinutes));
                if (freeMinutes >= Room.RESERVED_THRESHOLD_MINUTES) {
                    this.setBackgroundColor(getResources().getColor(R.color.TrafficLightFree));
                    bookNowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.traffic_lights_button_green));
                    bookNowView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_green));
                } else {
                    this.setBackgroundColor(getResources().getColor(R.color.TrafficLightYellow));
                    bookNowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.traffic_lights_button_yellow));
                    bookNowView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_yellow));
                }
            }
            reservationInfoView.setVisibility(GONE);
            roomStatusInfoView.setVisibility(VISIBLE);
            bookNowView.setVisibility(VISIBLE);
        } else {
            // this.setBackgroundColor(getResources().getColor(R.color.TrafficLightReserved));
            this.setBackgroundColor(getResources().getColor(R.color.TrafficLightReserved));
            roomStatusView.setText(getContext().getString(R.string.reserved));
            bookNowView.setVisibility(GONE);
            setReservationInfo(room.getCurrentReservation(), room.getNextFreeSlot());
/*            roomTitleView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_yellow));
            roomStatusView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_yellow));
            roomStatusInfoView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_yellow));
            bookNowView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_yellow));
            reservationInfoView.setTextColor(getResources().getColorStateList(R.color.traffic_lights_button_yellow)); */
/*            roomTitleView.setTextColor(getResources().getColorStateList(R.color.WhiteColor));
            roomStatusView.setTextColor(getResources().getColorStateList(R.color.WhiteColor));
            roomStatusInfoView.setTextColor(getResources().getColorStateList(R.color.WhiteColor));
            bookNowView.setTextColor(getResources().getColorStateList(R.color.WhiteColor));
            reservationInfoView.setTextColor(getResources().getColorStateList(R.color.WhiteColor)); */
        }
    }

    private void updateConnected() {
        if (this.isInEditMode()) {
            return;
        }
        ConnectivityManager cm = null;
        try {
            cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        } catch (ClassCastException cce) {
            return;
        }
        if (cm == null) return;

        NetworkInfo ni = cm.getActiveNetworkInfo();

        if (ni != null && ni.isConnectedOrConnecting()) {
            // Connected
            lastTimeConnected = new Date();
            if (disconnected.getVisibility() != GONE) {
                disconnected.setVisibility(GONE);
            }
        } else {
            // Disconnected
            if (lastTimeConnected.before(new Date(new Date().getTime() - DISCONNECTED_WARNING_ICON_THRESHOLD))) {
                if (disconnected.getVisibility() != VISIBLE) {
                    disconnected.setVisibility(VISIBLE);
                }
            }
        }
    }

    private void setReservationInfo(Reservation r, TimeSpan nextFreeSlot) {
        if (r == null) {
            roomStatusInfoView.setVisibility(GONE);
        } else {
            roomStatusInfoView.setText(r.getSubject());
            roomStatusInfoView.setVisibility(VISIBLE);
        }

        if (nextFreeSlot == null) {
            // More than a day away
            reservationInfoView.setVisibility(GONE);
        } else {
            reservationInfoView.setText(Html.fromHtml(String.format("Free at <b>%02d:%02d</b>",
                nextFreeSlot.getStart().get(Calendar.HOUR_OF_DAY),
                nextFreeSlot.getStart().get(Calendar.MINUTE))));
            reservationInfoView.setVisibility(VISIBLE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean capture = false;
        if (getVisibility() == VISIBLE) {
            setVisibility(INVISIBLE);
            capture = true;
            if (enabled) scheduleTimer();
        }
        lastTouched = new Date().getTime();
        return capture;
    }

    public void disable() {
        enabled = false;
        setVisibility(INVISIBLE);
        lastTouched = new Date().getTime();
        descheduleTimer();
    }

    public void enable() {
        enabled = true;
        lastTouched = new Date().getTime();
        scheduleTimer();
    }

    private void scheduleTimer() {
        if (touchTimeoutTimer == null) {
            touchTimeoutTimer = new Timer();
            touchTimeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (RoomTrafficLights.this.enabled &&
                        new Date().getTime() >= RoomTrafficLights.this.lastTouched + TOUCH_TIMEOUT) {
                        RoomTrafficLights.this.post(new Runnable() {
                            public void run() {
                                RoomTrafficLights.this.setVisibility(VISIBLE);
                            }
                        });
                        descheduleTimer();
                    }
                }
            }, TOUCH_TIMER, TOUCH_TIMEOUT);
        }
    }

    private void descheduleTimer() {
        if (touchTimeoutTimer != null) {
            touchTimeoutTimer.cancel();
            touchTimeoutTimer = null;
        }
    }
}
