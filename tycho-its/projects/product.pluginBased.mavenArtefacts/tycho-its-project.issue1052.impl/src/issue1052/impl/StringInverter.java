package issue1052.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;

import issue1052.api.StringModifier;

//The JAX-RS path annotation for this service
@Path("/inverter")
@Component(
	immediate = true,
	property = {
		// property to configure that the service must be processed by the JAX-RS Whiteboard 
		"osgi.jaxrs.resource=true"})
public class StringInverter implements StringModifier {

	@GET
    // The JAX-RS annotation to specify the result type
	@Produces(MediaType.TEXT_PLAIN)
    // The JAX-RS annotation to specify that the last part
    // of the URL is used as method parameter
	@Path("/{value}")
    @Override
    public String modify(@PathParam("value") String input) {
        return (input != null) 
        	? new StringBuilder(input).reverse().toString()
        	: "No input given";
    }
}