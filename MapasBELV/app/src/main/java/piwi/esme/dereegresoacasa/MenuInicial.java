package piwi.esme.dereegresoacasa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class MenuInicial extends AppCompatActivity {
    //Declaramos las variables que el usario ingresará en la pantalla inicial o en menu
    EditText la, lo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_inicial);
        //Abrimos el layout de menu inicial para que pueda ser ingresado la log y lat
        //buscamos en los recursos
        la = findViewById(R.id.editText8);
        lo = findViewById(R.id.editText5);
    }
    //Creamos un método para el botón para que cuando le den en aceptar,
    //se registren los datos en las variables
    public void aceptar(View view){
        //Creamos la variable latitud y la convertimos en cadena
        double latitud =Double.parseDouble(la.getText().toString());
        //sacamos los datos de la clase variable
        Variables.latitud =latitud;
        Variables.longitud = Double.parseDouble(lo.getText().toString());
        //Creamos el intent, le damos el contexto y mandamos a la actividad que sigue que es la
        //clase Maps activity
        Intent intent= new Intent(MenuInicial.this, MapsActivity.class);
        //Se inicia la actividad dado el intent
        startActivity(intent);

    }
}
