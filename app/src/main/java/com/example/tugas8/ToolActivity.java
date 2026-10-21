package com.example.tugas8;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class ToolActivity extends AppCompatActivity implements SensorEventListener, LocationListener {
    public static final String EXTRA_TOOL = "tool";
    public static final String TOOL_QIBLA = "qibla";
    public static final String TOOL_ALTITUDE = "altitude";
    private static final int LOCATION_PERMISSION_REQUEST = 42;
    private static final double KAABA_LATITUDE = 21.422487;
    private static final double KAABA_LONGITUDE = 39.826206;
    private static final float STANDARD_PRESSURE = 1013.25f;

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private Sensor accelerometer;
    private Sensor magneticField;
    private Sensor rotationVector;
    private Sensor pressureSensor;

    private CompassView compassView;
    private LevelBarView levelBarView;
    private TextView locationValue;
    private TextView qiblaValue;
    private TextView compassStatus;
    private TextView altitudeValue;
    private TextView relativeAltitudeValue;
    private TextView pressureValue;
    private TextView altitudeStatus;
    private MaterialCardView qiblaCard;
    private MaterialCardView altitudeCard;
    private boolean qiblaMode;

    private final float[] gravity = new float[3];
    private final float[] magnetic = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] inclinationMatrix = new float[9];
    private final float[] orientation = new float[3];
    private boolean hasGravity;
    private boolean hasMagnetic;
    private float heading;
    private float qiblaBearing;
    private boolean hasLocation;
    private float baselinePressure = Float.NaN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        qiblaMode = TOOL_QIBLA.equals(getIntent().getStringExtra(EXTRA_TOOL));
        getWindow().setStatusBarColor(Color.rgb(7, 35, 44));
        getWindow().setNavigationBarColor(Color.rgb(7, 35, 44));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        }
        buildInterface();
    }

    private void buildInterface() {
        int navy = Color.rgb(7, 35, 44);
        int teal = Color.rgb(23, 142, 135);
        int textPrimary = Color.rgb(27, 48, 53);
        int textSecondary = Color.rgb(91, 112, 116);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(246, 250, 248));
        scrollView.addView(root);

        TextView eyebrow = text("TOWER 2  /  SENSOR ASSIST", 12, teal, true);
        root.addView(eyebrow);
        TextView title = text("Arah & Ketinggian", 30, navy, true);
        title.setPadding(0, dp(5), 0, 0);
        root.addView(title);
        TextView subtitle = text("Panduan posisi berbasis sensor perangkat", 14, textSecondary, false);
        subtitle.setPadding(0, dp(2), 0, dp(18));
        root.addView(subtitle);

        qiblaCard = card();
        LinearLayout qiblaContent = vertical();
        qiblaContent.setPadding(dp(18), dp(18), dp(18), dp(18));
        qiblaContent.addView(sectionLabel("ARAH KIBLAT", teal));
        compassView = new CompassView(this);
        qiblaContent.addView(compassView, new LinearLayout.LayoutParams(-1, dp(240)));
        qiblaValue = text("Menunggu lokasi...", 22, navy, true);
        qiblaValue.setGravity(Gravity.CENTER);
        qiblaContent.addView(qiblaValue);
        compassStatus = text("Aktifkan lokasi untuk menentukan arah kiblat.", 13, textSecondary, false);
        compassStatus.setGravity(Gravity.CENTER);
        compassStatus.setPadding(0, dp(5), 0, 0);
        qiblaContent.addView(compassStatus);
        locationValue = text("Lokasi belum tersedia", 12, textSecondary, false);
        locationValue.setGravity(Gravity.CENTER);
        locationValue.setPadding(0, dp(8), 0, 0);
        qiblaContent.addView(locationValue);
        qiblaCard.addView(qiblaContent);
        root.addView(qiblaCard, marginParams(0, 0, 0, 14));

        altitudeCard = card();
        LinearLayout altitudeContent = vertical();
        altitudeContent.setPadding(dp(18), dp(18), dp(18), dp(18));
        altitudeContent.addView(sectionLabel("LEVEL KETINGGIAN", teal));
        LinearLayout metrics = new LinearLayout(this);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        altitudeValue = text("-- m", 32, navy, true);
        metrics.addView(altitudeValue, new LinearLayout.LayoutParams(0, -2, 1));
        levelBarView = new LevelBarView(this);
        metrics.addView(levelBarView, new LinearLayout.LayoutParams(dp(52), dp(132)));
        altitudeContent.addView(metrics);
        relativeAltitudeValue = text("Perubahan dari titik awal: --", 14, textSecondary, false);
        relativeAltitudeValue.setPadding(0, dp(5), 0, 0);
        altitudeContent.addView(relativeAltitudeValue);
        pressureValue = text("Tekanan: -- hPa", 13, textSecondary, false);
        pressureValue.setPadding(0, dp(4), 0, 0);
        altitudeContent.addView(pressureValue);
        altitudeStatus = text("Sensor barometer sedang disiapkan.", 13, textSecondary, false);
        altitudeStatus.setPadding(0, dp(12), 0, dp(12));
        altitudeContent.addView(altitudeStatus);
        Button calibrate = new Button(this);
        calibrate.setText("Jadikan titik ini sebagai titik awal");
        calibrate.setTextColor(Color.WHITE);
        calibrate.setBackgroundColor(teal);
        calibrate.setOnClickListener(v -> {
            baselinePressure = Float.NaN;
            altitudeStatus.setText("Titik awal diatur ulang. Diamkan perangkat sesaat.");
        });
        altitudeContent.addView(calibrate, new LinearLayout.LayoutParams(-1, dp(46)));
        altitudeCard.addView(altitudeContent);
        root.addView(altitudeCard, marginParams(0, 0, 0, 14));

        TextView note = text("Letakkan perangkat mendatar untuk hasil kompas paling stabil. "
                + "Ketinggian adalah estimasi dari tekanan udara dan dapat berubah karena cuaca.", 12, textSecondary, false);
        note.setPadding(dp(4), 0, dp(4), 0);
        root.addView(note);
        qiblaCard.setVisibility(qiblaMode ? View.VISIBLE : View.GONE);
        altitudeCard.setVisibility(qiblaMode ? View.GONE : View.VISIBLE);
        note.setText(qiblaMode
                ? "Letakkan perangkat mendatar dan gerakkan membentuk angka 8 jika kompas kurang akurat."
                : "Ketinggian adalah estimasi dari tekanan udara dan dapat berubah karena cuaca.");
        setContentView(scrollView);
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(22));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.rgb(224, 235, 232));
        card.setCardBackgroundColor(Color.WHITE);
        return card;
    }

    private TextView sectionLabel(String value, int color) {
        return text(value, 12, color, true);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(size);
        result.setTextColor(color);
        result.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return result;
    }

    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager == null) {
            return;
        }
        if (qiblaMode) {
            if (rotationVector != null) {
                sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI);
            } else {
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                }
                if (magneticField != null) {
                    sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI);
                }
            }
        } else {
            if (pressureSensor != null) {
                sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI);
                altitudeStatus.setText("Barometer aktif. Ketinggian dihitung dari tekanan udara.");
            } else {
                altitudeStatus.setText("Perangkat tidak memiliki sensor barometer.");
            }
        }
        if (qiblaMode) {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (qiblaMode && locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private void requestLocationUpdates() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            compassStatus.setText("Izin lokasi diperlukan untuk menghitung arah kiblat.");
            return;
        }
        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                compassStatus.setText("Aktifkan lokasi perangkat untuk arah kiblat.");
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2, this);
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnown != null) {
                onLocationChanged(lastKnown);
            }
        } catch (SecurityException exception) {
            compassStatus.setText("Lokasi tidak dapat diakses. Periksa izin aplikasi.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            updateHeading(rotationMatrix);
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3);
            hasGravity = true;
            updateFusedHeading();
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetic, 0, 3);
            hasMagnetic = true;
            updateFusedHeading();
        } else if (type == Sensor.TYPE_PRESSURE) {
            updateAltitude(event.values[0]);
        }
    }

    private void updateFusedHeading() {
        if (hasGravity && hasMagnetic
                && SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, magnetic)) {
            updateHeading(rotationMatrix);
        }
    }

    private void updateHeading(float[] matrix) {
        SensorManager.getOrientation(matrix, orientation);
        heading = (float) Math.toDegrees(orientation[0]);
        if (heading < 0) {
            heading += 360;
        }
        compassView.setHeading(heading, qiblaBearing);
        if (hasLocation) {
            float relative = normalizeDegrees(qiblaBearing - heading);
            qiblaValue.setText(String.format(Locale.getDefault(), "%.0f°  %s", qiblaBearing, directionName(qiblaBearing)));
            compassStatus.setText(String.format(Locale.getDefault(),
                    "Putar perangkat %.0f° %s menuju penunjuk kiblat.",
                    Math.abs(relative), relative >= 0 ? "searah jarum jam" : "berlawanan arah jarum jam"));
        }
    }

    private void updateAltitude(float pressure) {
        if (Float.isNaN(baselinePressure)) {
            baselinePressure = pressure;
        }
        float seaLevelAltitude = SensorManager.getAltitude(STANDARD_PRESSURE, pressure);
        float relativeAltitude = SensorManager.getAltitude(baselinePressure, pressure);
        altitudeValue.setText(String.format(Locale.getDefault(), "%.1f m", seaLevelAltitude));
        relativeAltitudeValue.setText(String.format(Locale.getDefault(),
                "Perubahan dari titik awal: %+.1f m", relativeAltitude));
        pressureValue.setText(String.format(Locale.getDefault(), "Tekanan: %.1f hPa", pressure));
        levelBarView.setLevel(relativeAltitude);
    }

    private float normalizeDegrees(float value) {
        while (value > 180) value -= 360;
        while (value < -180) value += 360;
        return value;
    }

    private String directionName(float degrees) {
        String[] directions = {"U", "UT", "T", "TG", "S", "BD", "B", "BL"};
        return directions[Math.round(degrees / 45f) % 8];
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = Math.toRadians(location.getLatitude());
        double longitude = Math.toRadians(location.getLongitude());
        double kaabaLatitude = Math.toRadians(KAABA_LATITUDE);
        double deltaLongitude = Math.toRadians(KAABA_LONGITUDE) - longitude;
        double y = Math.sin(deltaLongitude) * Math.cos(kaabaLatitude);
        double x = Math.cos(latitude) * Math.sin(kaabaLatitude)
                - Math.sin(latitude) * Math.cos(kaabaLatitude) * Math.cos(deltaLongitude);
        qiblaBearing = (float) ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
        hasLocation = true;
        locationValue.setText(String.format(Locale.getDefault(), "Lokasi: %.4f, %.4f",
                location.getLatitude(), location.getLongitude()));
        updateHeading(rotationMatrix);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        compassStatus.setText("Penyedia lokasi dimatikan.");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor != null && sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            compassStatus.setText("Kompas kurang akurat. Gerakkan perangkat membentuk angka 8.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                compassStatus.setText("Izin lokasi ditolak. Arah kiblat belum dapat dihitung.");
                Toast.makeText(this, "Aktifkan izin lokasi dari Pengaturan.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class CompassView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float currentHeading;
        private float targetBearing;

        CompassView(Context context) {
            super(context);
            paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        }

        void setHeading(float heading, float bearing) {
            currentHeading = heading;
            targetBearing = bearing;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(centerX, centerY) - dp(12);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(232, 245, 241));
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(23, 142, 135));
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(178, 210, 204));
            canvas.drawCircle(centerX, centerY, radius - dp(14), paint);

            canvas.save();
            canvas.rotate(-currentHeading, centerX, centerY);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(7, 35, 44));
            paint.setTextSize(dp(18));
            canvas.drawText("U", centerX - dp(7), centerY - radius + dp(29), paint);
            canvas.drawText("S", centerX - dp(7), centerY + radius - dp(15), paint);
            canvas.drawText("B", centerX - radius + dp(16), centerY + dp(7), paint);
            canvas.drawText("T", centerX + radius - dp(26), centerY + dp(7), paint);
            canvas.restore();

            float qiblaAngle = normalizeDegrees(targetBearing - currentHeading);
            canvas.save();
            canvas.rotate(qiblaAngle, centerX, centerY);
            paint.setColor(Color.rgb(231, 126, 45));
            paint.setStyle(Paint.Style.FILL);
            PathTriangle.draw(canvas, centerX, centerY, radius - dp(23), dp(12), paint);
            canvas.restore();
            paint.setColor(Color.rgb(231, 126, 45));
            canvas.drawCircle(centerX, centerY, dp(7), paint);
        }
    }

    private static class PathTriangle {
        static void draw(Canvas canvas, float x, float y, float length, float width, Paint paint) {
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(x, y - length);
            path.lineTo(x - width, y - length + width * 2);
            path.lineTo(x + width, y - length + width * 2);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private class LevelBarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float level;

        LevelBarView(Context context) {
            super(context);
        }

        void setLevel(float value) {
            level = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float left = dp(16);
            float top = dp(8);
            float width = dp(20);
            float height = getHeight() - dp(16);
            paint.setColor(Color.rgb(224, 239, 235));
            canvas.drawRoundRect(new RectF(left, top, left + width, top + height), dp(10), dp(10), paint);
            float normalized = Math.max(0.08f, Math.min(1f, 0.5f + level / 40f));
            paint.setColor(Color.rgb(23, 142, 135));
            float filledTop = top + height * (1 - normalized);
            canvas.drawRoundRect(new RectF(left, filledTop, left + width, top + height), dp(10), dp(10), paint);
            paint.setTextSize(dp(11));
            paint.setColor(Color.rgb(91, 112, 116));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("m", left + width / 2, top + height + dp(14), paint);
        }
    }
}
