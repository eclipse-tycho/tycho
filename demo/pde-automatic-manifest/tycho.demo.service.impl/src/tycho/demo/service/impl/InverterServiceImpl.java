package tycho.demo.service.impl;

import javax.inject.Singleton;
import javax.inject.Named;
import org.osgi.service.component.annotations.Component;

import tycho.demo.service.api.InverterService;

@Named
@Singleton
public class InverterServiceImpl implements InverterService {

    @Override
    public String invert(String input) {
	    return new StringBuilder(input).reverse().toString();
    }
}