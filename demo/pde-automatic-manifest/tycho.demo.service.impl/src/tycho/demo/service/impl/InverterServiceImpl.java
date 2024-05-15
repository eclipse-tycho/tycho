package tycho.demo.service.impl;

import org.osgi.service.component.annotations.Component;

import tycho.demo.service.api.InverterService;

@Component
public class InverterServiceImpl implements InverterService {

    @Override
    public String invert(String input) {
	    return new StringBuilder(input).reverse().toString();
    }
}