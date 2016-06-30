package com.adobe.mysite.components.task.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.tagging.JcrTagManagerFactory;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pavel.kulikou on 6/29/2016.
 */
@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Test Tagging workflow process implementation."),
        @Property(name = Constants.SERVICE_VENDOR, value = "Adobe"),
        @Property(name = "process.label", value = "Test Tagging workflow") })
public class TaggingProcess implements WorkflowProcess {


    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    JcrTagManagerFactory tmf;

    private Session session;
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {

        try{
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            TagManager tagManager = tmf.getTagManager(session);
            Tag tagToSet[] = new Tag[1];
            StringBuilder payload = new StringBuilder();
             String processArgs = metaDataMap.get("PROCESS_ARGS", "default value");
            String[] proccesArgsVals = processArgs.split(";");

            HashMap<String, String[]> stepAttributes = parseStepAttributes(proccesArgsVals);


              tagToSet[0] = tagManager.createTag((stepAttributes).get("tagName")[0], "MyTagTitle", "MyTagDescription");//tagManager.createTagByTitle("MyTagTitle", true);

            payload.append(workItem.getWorkflowData().getPayload().toString());
            Resource imageResource = resourceResolver.getResource(payload.substring(0, payload.length()-19).concat("metadata"));

            if(checkKeyWords(stepAttributes, imageResource)){
                tagManager.setTags(imageResource, tagToSet, true);
            }

            session.save();
            session.logout();

        }
        catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }


    }

    private boolean checkKeyWords(Map<String, String[]> stepAttributes, Resource imageResource) {
        String fileName = imageResource.getParent().getParent().getName().toLowerCase();
        boolean result = false;
        for(String key : stepAttributes.get("keyWords")){
            if(fileName.indexOf(key.toLowerCase())!=-1) {
                result = true;
                break;
            }
        }
        return result;
    }

    private HashMap<String, String[]> parseStepAttributes(String[] proccesArgsVals) {
        HashMap<String, String[]> values = new HashMap<>();
        for(int i=0; i<proccesArgsVals.length; i++){
            String[] s = proccesArgsVals[i].replaceAll(" ", "").split("=");
            String[] v = s[1].replaceAll("\\[","").replaceAll("\\]", "").replaceAll(" ", "").split(",");
            values.put(s[0], v);
        }
        return values;
    }
}
