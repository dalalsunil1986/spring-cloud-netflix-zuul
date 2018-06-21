package com.story.rest;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.story.domain.Story;
import com.story.service.StoryService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

@CrossOrigin(origins ="*")
@RestController
public class StoryController {

	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DiscoveryClient discoveryClient;

    @Bean
    public RestTemplate restTemplate() {
    return new RestTemplate();
    }
	@Value("${eureka.instance.instance-id}")
	String instance_id;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	StoryService storyService;
    
    @HystrixCommand(commandKey="story-hostname", fallbackMethod = "getHostNameFallback")
    @RequestMapping(value = "/storys", method = RequestMethod.GET)
    public String checkHost()
    {
        try{
          
            logger.info("input !!");
           return System.getenv("HOSTNAME");

        }
        catch(Exception e)
        {
            return "Cannot find host ";
        }
    
    }
    public String getHostNameFallback()
    {

        return "HHHHHHHHH";
    }

	@HystrixCommand(commandKey = "story-get", fallbackMethod = "getAllStoryFallback")
	@RequestMapping(value = "/story", method = RequestMethod.GET)
	public ResponseEntity<List<Story>> getAllStory()
	{
		try{

		    Optional<List<Story>> maybeAllStory = Optional.of(storyService.findAllStory());

		    return new ResponseEntity<List<Story>>(maybeAllStory.get(), HttpStatus.OK);

        }catch(Exception e)
        {
            return new ResponseEntity<List<Story>>(HttpStatus.NOT_FOUND);
        }
	}


    public ResponseEntity<List<Story>>  getAllStoryFallback()
    {
        try{

            Optional<List<ServiceInstance>> maybeServiceInstance 
                = Optional.of(this.discoveryClient.getInstances("story-service"));
        
            List<ServiceInstance> Instance 
                = maybeServiceInstance.get().stream()
                                            .filter(service -> 
                                                    !instance_id.equals(service.getServiceId()))
                                            .collect(Collectors.toList());

            ServiceInstance service = Instance.get(0);
        
            URI uri = URI.create(service.getHost() + ":" + service.getPort() + "/story");

            ResponseEntity <List<Story>> rest =
				    restTemplate.exchange(uri, HttpMethod.GET,null, new ParameterizedTypeReference<List<Story>>() {});
            
            return new ResponseEntity<List<Story>>(rest.getBody(), HttpStatus.OK);

        }catch(Exception e)
        {
			e.printStackTrace();
        }
        
        return null;
    }


	@RequestMapping(value = "/story/{id}", method = RequestMethod.GET)
	public ResponseEntity<List<Story>> getStoryById(@PathVariable("id") final String ID)
	{

		try {
			
			Optional <List<Story>> maybeStoryById = Optional.of(storyService.findStoryById(ID));
			
			return new ResponseEntity<List<Story>>(maybeStoryById.get(),HttpStatus.OK);
			
		}catch(Exception e)
		{
			return new ResponseEntity<List<Story>>(HttpStatus.NOT_FOUND);
		}
		
	}
	
	@RequestMapping(value = "/story", method = RequestMethod.POST)
	public ResponseEntity<Void> createStory(@RequestBody final Story story, final UriComponentsBuilder ucBuilder)
	{
		if(!storyService.saveStory(story)){ return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);}
		
		return new ResponseEntity<Void>(HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/story/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteStory(@PathVariable("id") final String ID)
	{
		if(!storyService.deleteStory(ID)){ return new ResponseEntity<Void>(HttpStatus.NOT_FOUND); }

		return new ResponseEntity<Void>(HttpStatus.OK);
		
	}
}















