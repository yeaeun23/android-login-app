package kr.co.seoul;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

public class soapHandler implements Callable<String> {
    private String SOAP_ACTION, METHOD_NAME, URL;
    private Map<String, String> map;

    soapHandler(String strSoapAction, String strMethodName, String strUrl, Map<String, String> map) {
        SOAP_ACTION = strSoapAction;
        METHOD_NAME = strMethodName;
        URL = strUrl;
        this.map = map;
    }

    public String call() {
        SoapObject request = new SoapObject("http://tempuri.com/", METHOD_NAME);
        Iterator<String> iterator = map.keySet().iterator();
        String strKey, strValue;

        while (iterator.hasNext()) {
            strKey = iterator.next();
            strValue = map.get(strKey);

            if (!strKey.equals("")) {
                request.addProperty(strKey, strValue);
            }
        }

        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = true;
        envelope.setOutputSoapObject(request);

        try {
            new HttpTransportSE(URL).call(SOAP_ACTION, envelope);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ((SoapObject) envelope.bodyIn).getProperty(METHOD_NAME + "Result").toString();
    }
}
