package com.abc.util.wsdl;

import com.predic8.wsdl.*;
import com.predic8.wstool.creator.RequestTemplateCreator;
import com.predic8.wstool.creator.SOARequestCreator;
import groovy.xml.MarkupBuilder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class WsdlUtils {
    private static final String wsdl = "http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl";
    private static final Logger logger = LoggerFactory.getLogger(WsdlUtils.class);

    @Data
    public static class SoapPortType {
        private String bindingName;
        private String portTypeName;
        private List<String> operationNames;
    }

    public static SoapPortType parse(String url) {
        SoapPortType soapPortType = new SoapPortType();

        WSDLParser parser = new WSDLParser();
        Definitions wsdl = parser.parse(url);

        List<PortType> portTypes = wsdl.getPortTypes();
        soapPortType.setPortTypeName(portTypes.get(0).getName());
        for (PortType portType : portTypes) {
            logger.info("portType name: {}", portType);
            List<Operation> operations = portType.getOperations();
            List<String> operationNames = new ArrayList<>(operations.size());
            soapPortType.setOperationNames(operationNames);
            for (Operation operation : operations) {
                operationNames.add(operation.getName());
                logger.info("operation {}", operation.getName());
            }
        }

        List<Binding> bindings = wsdl.getBindings();
        soapPortType.setBindingName(bindings.get(0).getName());
        for (Binding binding : bindings) {
            logger.info("binding {}", binding);
        }
        return soapPortType;
    }

    public static String createSOAPTemplate(String url, String operationName) {
        SoapPortType soapPortType = parse(url);
        return createTemplates(url, soapPortType.getBindingName(), soapPortType.getPortTypeName(), operationName);
    }

    public static String createTemplates(String url, String bindingName, String portTypeName, String operationName) {
        WSDLParser parser = new WSDLParser();
        Definitions wsdl = parser.parse(url);

        StringWriter writer = new StringWriter();
        SOARequestCreator creator = new SOARequestCreator(wsdl, new RequestTemplateCreator(), new MarkupBuilder(writer));
        logger.info("{}, {}, {}", portTypeName, operationName, bindingName);
        creator.createRequest(portTypeName, operationName, bindingName);
        return writer.toString();
    }

    public static void main(String[] args) throws Exception {
        String wsdl = "http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl";
        SoapPortType soapPortType = WsdlUtils.parse(wsdl);
        System.out.println(WsdlUtils.createSOAPTemplate(wsdl, soapPortType.getOperationNames().get(0)));
        System.out.println(soapPortType);
    }

}
