package com.xryrapier.ifinbot;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private EditText apiUrl, age, kids, income, networth, investment, ndays;
    private Spinner sex, education, married, family, occupation, savings, financialRisk;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("iFinbot");
        title.setTextSize(28);
        root.addView(title);

        apiUrl = input(root, "API URL", "http://10.0.2.2:8000");
        sex = spinner(root, "Sexo", new String[]{"1 - Hombre", "2 - Mujer"});
        age = input(root, "Edad", "35");
        education = spinner(root, "Nivel de estudios", new String[]{"1 - Sin high school", "2 - High school", "3 - Some college", "4 - College degree"});
        married = spinner(root, "Estado civil", new String[]{"1 - Casado", "2 - No casado"});
        kids = input(root, "Numero de hijos", "0");
        family = spinner(root, "Estructura familiar", new String[]{"1 - No casado con hijos", "2 - No casado sin hijos <55", "3 - No casado sin hijos >=55", "4 - Casado con hijos", "5 - Casado sin hijos"});
        occupation = spinner(root, "Ocupacion", new String[]{"1 - Empleado", "2 - Autoempleado", "3 - Retirado/estudiante/etc", "4 - Otro sin trabajo <65"});
        income = input(root, "Ingresos", "85000");
        savings = spinner(root, "Ahorros", new String[]{"1 - Gasta mas de lo que gana", "2 - Gasta igual", "3 - Ahorra"});
        financialRisk = spinner(root, "Tomaria mas riesgo financiero", new String[]{"0 - No", "1 - Si"});
        networth = input(root, "Patrimonio neto", "250000");
        investment = input(root, "Monto a invertir", "100000");
        ndays = input(root, "Dias historicos", "180");

        Button submit = new Button(this);
        submit.setText("Calcular portafolio");
        root.addView(submit);

        output = new TextView(this);
        output.setTextSize(15);
        output.setPadding(0, 24, 0, 0);
        root.addView(output);

        submit.setOnClickListener(v -> calculate());
        setContentView(scroll);
    }

    private EditText input(LinearLayout root, String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label);
        root.addView(tv);
        EditText edit = new EditText(this);
        edit.setText(value);
        edit.setSingleLine(true);
        root.addView(edit);
        return edit;
    }

    private Spinner spinner(LinearLayout root, String label, String[] values) {
        TextView tv = new TextView(this);
        tv.setText(label);
        root.addView(tv);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        root.addView(sp);
        return sp;
    }

    private int code(Spinner spinner) {
        return Integer.parseInt(spinner.getSelectedItem().toString().substring(0, 1));
    }

    private void calculate() {
        output.setText("Calculando...");
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("HHSEX", code(sex));
                body.put("AGE", Integer.parseInt(age.getText().toString()));
                body.put("EDCL", code(education));
                body.put("MARRIED", code(married));
                body.put("KIDS", Integer.parseInt(kids.getText().toString()));
                body.put("FAMSTRUCT", code(family));
                body.put("OCCAT1", code(occupation));
                body.put("INCOME", Double.parseDouble(income.getText().toString()));
                body.put("WSAVED", code(savings));
                body.put("YESFINRISK", code(financialRisk));
                body.put("NETWORTH", Double.parseDouble(networth.getText().toString()));
                body.put("investment", Double.parseDouble(investment.getText().toString()));
                body.put("ndays", Integer.parseInt(ndays.getText().toString()));

                URL url = new URL(apiUrl.getText().toString() + "/api/portfolio");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);

                if (conn.getResponseCode() >= 400) {
                    throw new RuntimeException(response.toString());
                }
                showResult(new JSONObject(response.toString()));
            } catch (Exception e) {
                mainHandler.post(() -> output.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void showResult(JSONObject response) throws Exception {
        JSONObject client = response.getJSONObject("client");
        JSONArray portfolio = response.getJSONArray("portfolio");
        StringBuilder text = new StringBuilder();
        text.append("Perfil: ").append(client.getString("risk_profile")).append("\n");
        text.append("RT: ").append(String.format("%.2f%%", client.getDouble("rt_score") * 100)).append("\n");
        text.append("Volatilidad objetivo: ").append(String.format("%.2f%%", client.getDouble("target_annual_volatility") * 100)).append("\n\n");
        text.append("Portafolio\n");
        for (int i = 0; i < portfolio.length(); i++) {
            JSONObject item = portfolio.getJSONObject(i);
            text.append(item.optString("Ticker"))
                    .append("  acciones: ").append(item.optString("Number of actions"))
                    .append("  peso: ").append(item.optString("Weight"))
                    .append("\n");
        }
        mainHandler.post(() -> output.setText(text.toString()));
    }
}
