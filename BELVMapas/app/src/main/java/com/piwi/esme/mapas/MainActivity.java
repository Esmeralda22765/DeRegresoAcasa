package com.piwi.esme.mapas;


import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, RoutingListener {

/*
//GoogleAPIClient
API Client proporciona un punto de entrada común a los
servicios de Google Play y administra la conexión de red entre
el dispositivo del usuario y cada servicio de Google

//OnConectionFaildeListener
Proporciona devoluciones de llamada para escenarios
que resultan en un intento fallido de conectar al
cliente al servicio

//RoutingListener
proporciona un método para funciones de devolución de llamada.
Cuando un RoutingComponent recibe una llamada asincrónica para una ruta,
 espera que RouteListener vuelva a llamar.
 Una vez calculada la ruta, llama al método onRouteCalculated
 para informar al componente de devolución de llamada.

//OnMapRaedyCallback
Devolución de llamada para cuando el mapa esté listo para usarse.

//Fragmentactivity
Es una actividad especial proporcionada en la biblioteca de
compatibilidad para manejar fragmentos en versiones del
sistema anteriores a la API
     */



    //objeto de google map
    private GoogleMap mMap;

    //objetos de ubicación actual y del destino
    Location myLocation = null;
    Location destinationLocation = null;
    protected LatLng start = null;//
    protected LatLng end = null;

    //Para obtener los permisos de ubicación
    private final static int LOCATION_REQUEST_CODE = 23;
    boolean locationPermission = false;

    //Objeto polyline
    private List<Polyline> polylines = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recupera la vista de contenido que representa el mapa.
        setContentView(R.layout.activity_main);

        //Solicitar permiso de ubicación
        requestPermision();

        //init fragmento de mapa de Google para mostrar el mapa.
        //Obtiene SupportMapFragment y solicita una notificación cuando el mapa esté listo para ser utilizado.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //verifica los permisos
    //el Acces_coarse_loction Permite que una aplicación acceda a la ubicación aproximada
    private void requestPermision() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            locationPermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Si es otorgado el permiso, obtiene la ubicacion
                    locationPermission = true;
                    getMyLocation();

                } else {
                    // permiso denegado

                }
                return;
            }
        }
    }

    //Para obtener la ubicación del usuario
    private void getMyLocation() {

        mMap.setMyLocationEnabled(true); //habilita mi ubicación
        //devolución de llamada para cuando el punto de mi ubicación
        // (que significa la ubicación del usuario) cambia de ubicación.
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                myLocation=location;

                //se obtiene la latitud y longitud
                LatLng ltlng=new LatLng(location.getLatitude(),location.getLongitude());

                //Define un movimiento de cámara. Un objeto de este tipo se puede utilizar
                // para modificar la cámara de un mapa.
               // newLatLngZoom devuelve CameraUpdate que mueve el centro de la pantalla a una latitud
               // y longitud especificadas por un objeto LatLng y se mueve al nivel de zoom dado.
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                        ltlng, 16f);

                /*

                Anima el movimiento de la cámara desde la posición actual a la
                 posición definida en la actualización.
                 */
                mMap.animateCamera(cameraUpdate);
            }
        });

        //Obtiene la ubicacion de otro punto al dar clic en el mapa
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                end=latLng;

                mMap.clear();

                start=new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
                //Empieza a buscar la ruta
                Findroutes(start,end);
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    //si los permisos son otorgados obtiene mi ubicacion
        if(locationPermission) {
            getMyLocation();
        }

    }


    // Funcion para encontrar rutas
    public void Findroutes(LatLng Start, LatLng End)
    {
        //comienza obteniendo la ubicacion del punto seleccionado desde mi ubicacion actual hasta el destino
        if(Start==null || End==null) {
            //si no se pudo encontrar la ruta manda mensaje de que no se pudo encontrar la ruta
            Toast.makeText(MainActivity.this,"Error al encontrar rutas", Toast.LENGTH_LONG).show();
        }
        else
        {

            //construye la ruta
            //travel mode especifica qué modo de transporte utilizar al calcular las direcciones.
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING) //permite que se puedan planificar diferentes rutas
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)//alteran una ruta al enrutarla a través de las ubicaciones especificadas.
                    .key("AIzaSyCqnKhtKHYjuXFDFO36d05Vj5BXds7z79A")  //API.
                    .build();
            routing.execute();//ejecuta la rut
        }
    }

    //Llamar las funciones.
    @Override
    public void onRoutingFailure(RouteException e) {
        //Utiliza las API del paquete android.animation
        View parentLayout = findViewById(android.R.id.content);

        //Es un nuevo widget introducido con la biblioteca Material Design como reemplazo de Toast.
        // Android Snackbar es un widget
        // liviano y se usa para mostrar mensajes en la parte inferior de la aplicación con el deslizamiento habilitado.
        Snackbar snackbar= Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG);
        snackbar.show();
//        Findroutes(start,end);
    }

    @Override
    public void onRoutingStart() {
        //manda mensaje si la ruta es encontrada
        Toast.makeText(MainActivity.this,"Encontrando ruta...",Toast.LENGTH_LONG).show();
    }

    //Si la ruta es encontrada, comienza a dibujar el trazo.
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
//Define un movimiento de cámara. Un objeto de este tipo
// se puede utilizar para modificar la cámara de un mapa.

        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

        //cada que se selecciona un punto diferente
        // elimina todos los elementos, pero las polilíneas en Google Map siguen siendo visibles
        if(polylines!=null) {
            polylines.clear();
        }

        /*
Una polilínea es una lista de puntos, donde se dibujan segmentos de línea entre puntos consecutivos.
Una polilínea tiene las siguientes propiedades:

Puntos:
Los vértices de la línea. Los segmentos de línea se dibujan entre puntos consecutivos.
Una polilínea no está cerrada de forma predeterminada; para formar una polilínea cerrada,
los puntos inicial y final deben ser iguales.

Ancho:
Ancho del segmento de línea en píxeles de la pantalla. El ancho es constante e independiente
del nivel de zoom de la cámara. El valor predeterminado es 10.
         */
        PolylineOptions polyOptions = new PolylineOptions();//permite elegir el color de la polilínea
        LatLng polylineStartLatLng=null;
        LatLng polylineEndLatLng=null;


        polylines = new ArrayList<>();
        //Agregando rutas usando Polylines a un arraylist
        for (int i = 0; i <route.size(); i++) {

            if(i==shortestRouteIndex) //busca la ruta más cernada
            {
                //color del trazo de la ruta, se obtiene de la carpeta values/colors
                polyOptions.color(getResources().getColor(R.color.colorPrimary));
                //el ancho de la polilínea en píxeles de pantalla
                polyOptions.width(7);

                //Cuando encuentra la ruta corta, agrega
                polyOptions.addAll(route.get(shortestRouteIndex).getPoints());
                //es entonces que traza la linea
                Polyline polyline = mMap.addPolyline(polyOptions);

                polylineStartLatLng=polyline.getPoints().get(0);//obtiene la ubicacion inicial
                int k=polyline.getPoints().size();//indica la cantidad de puntos
                polylineEndLatLng=polyline.getPoints().get(k-1);//obtiene la ubicacion final, donde termina la ruta menos
                //uno para ubicar la posicion
                polylines.add(polyline);//traza la ruta

            }
            else {

            }

        }

        //Agregar marca en la ubicacion de inicio
        MarkerOptions startMarker = new MarkerOptions();
        startMarker.position(polylineStartLatLng);
        startMarker.title("Mi ubicación");
        mMap.addMarker(startMarker);

        //Agregar marca en la posicion destino
        MarkerOptions endMarker = new MarkerOptions();
        endMarker.position(polylineEndLatLng);
        endMarker.title("Destino");
        mMap.addMarker(endMarker);
    }

    @Override
    public void onRoutingCancelled() {
        Findroutes(start,end);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Findroutes(start,end);

    }
}
