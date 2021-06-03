package piwi.esme.dereegresoacasa;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //Creamos un objeto de la clase GoogleMap
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //Inicializa los mapas
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    double lo, la;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        lo = Variables.longitud;
        la = Variables.latitud;
        Log.e("campos", lo + ", " + la);
        LatLng origen = new LatLng(la, lo);
        LatLng destino = new LatLng(40.7332825, -74.0424432);
        //LatLng destino = new LatLng(20.139695,-101.150705);

        mMap.addMarker(new MarkerOptions().position(origen).title("Origen"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(origen));
        mMap.addMarker(new MarkerOptions().position(destino).title("Destino"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(destino));

        String url = obtenerDireccionesURL(origen, destino);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    private String obtenerDireccionesURL(LatLng origin, LatLng dest) {
        //Se crea una variable de origen para obtener la direccion con ayuda de google maps
        //y claro de la api que implementamos en las dependencias
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        //Se crea una variable de destino para obtener la direccion con ayuda de google maps
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //Creación de una variable que sea la encargada de almacenar las dos anteriores
        //llamada parametros
        String parameters = str_origin + "&" + str_dest + "&";
        //creamos una variable llamada salida que sera el json
        String output = "json";
        //especificamos una URL para las direcciones en el maps por medio de la api
        // dando la APIKEY dada en clase por el docente
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?"
                + parameters + "&key=AIzaSyBHtYD_i3eqYqdCroUTQDwzb5FtqD323oc";
        Log.e("laurl", url);
        //lanzamos el resultado de la consulta a las direcciones
        return url;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        //Heredamos un método abstracto en donde vamos a pasar el url para que se pueda
        //realizar la consulta
        @Override
        protected String doInBackground(String... url) {
            //creamos variable para almacenar el resultado de la consulta
            String data = "";
            try {
                //descarga el URL para poder ser enviado y usar su info cosultada
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                //caso contrario, lanzamos un mensajito que fallo algo
                Log.d("ERROR AL OBTENER INFO", e.toString());
            }
            //regresamos dato
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
            //aqui le pasamos una variable resultado que es la encargada de
            //termiar el proceso de la clase parsertask
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
//creamos un objeto de la clase JSONObject
            JSONObject jObject;
            //para crear una lista de los puntos que se hizo en la consulta de arriba
            List<List<HashMap<String, String>>> routes = null;

            try {
                //buscamos en la consulta de JSON
                jObject = new JSONObject(jsonData[0]);
                //buscamos las direciones de tipo json y almacenamos
                DirectionsJSONParser parser = new DirectionsJSONParser();
                //se las pasamos a routes el objeto de tipo json
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //lanzamos los datos de la ruta
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(4);
                lineOptions.color(Color.rgb(0, 0, 255));
            }
            //if(lineOptions!=null) {
            mMap.addPolyline(lineOptions);
            //}
        }
    }

    public class DirectionsJSONParser {
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;
            try {
                jRoutes = jObject.getJSONArray("routes");
                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<HashMap<String, String>>();
                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }

            return routes;
        }

        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;
            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;
                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;
                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }
            return poly;
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creamos una conexion http
            urlConnection = (HttpURLConnection) url.openConnection();
            // Conectamos
            urlConnection.connect();
            // Leemos desde URL
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}