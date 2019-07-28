package com.abc.util.wsdl;

import com.predic8.wsdl.*;
import com.predic8.wstool.creator.RequestTemplateCreator;
import com.predic8.wstool.creator.SOARequestCreator;
import groovy.xml.MarkupBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

public class WsdlUtils {
    private static final String wsdl = "http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl";
    private static final Logger logger = LoggerFactory.getLogger(WsdlUtils.class);

    public static void createTemplates(String url) {
        WSDLParser parser = new WSDLParser();
        Definitions wsdl = parser.parse(url);
        StringWriter writer = new StringWriter();

        List<PortType> portTypes = wsdl.getPortTypes();
        String portTypeName = portTypes.get(0).getName();
        String operationName = null;
        for (PortType portType : portTypes) {
            logger.info("portType name: {}", portType);
            List<Operation> operations = portType.getOperations();
            for (Operation operation : operations) {
                if (operationName == null) {
                    operationName = operation.getName();
                }
                logger.info("operation {}", operation.getName());
            }
        }

        List<Binding> bindings = wsdl.getBindings();
        String firstBindName = bindings.get(0).getName();
        for (Binding binding : bindings) {
            logger.info("binding {}", binding);
        }

        SOARequestCreator creator = new SOARequestCreator(wsdl, new RequestTemplateCreator(), new MarkupBuilder(writer));
        logger.info("{}, {}, {}", portTypeName, operationName, firstBindName);
        Object request = creator.createRequest(portTypeName, operationName, firstBindName);
        System.out.println(writer);
    }

    public static void main(String[] args) throws Exception {
        WsdlUtils.createTemplates(wsdl);
    }

}
