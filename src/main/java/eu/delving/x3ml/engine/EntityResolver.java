/*==============================================================================
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
==============================================================================*/
package eu.delving.x3ml.engine;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import eu.delving.x3ml.X3MLEngine;
import java.util.ArrayList;
import java.util.List;
import static eu.delving.x3ml.engine.X3ML.Additional;
import static eu.delving.x3ml.engine.X3ML.GeneratedValue;
import static eu.delving.x3ml.engine.X3ML.GeneratorElement;
import eu.delving.x3ml.engine.X3ML.LabelGeneratorElement;
import static eu.delving.x3ml.engine.X3ML.TypeElement;
import gr.forth.Utils;
import java.util.Set;
import java.util.TreeSet;
import static eu.delving.x3ml.X3MLEngine.exception;

/**
 * The entity resolver creates the related model elements by calling generator
 * functions.
 * <p/>
 * Handles label nodes and additional nodes with their properties
 *
 * @author Gerald de Jong <gerald@delving.eu>
 * @author Nikos Minadakis <minadakn@ics.forth.gr>
 * @author Yannis Marketakis <marketak@ics.forth.gr>
 */
public class EntityResolver {

    public static ModelOutput modelOutput;
    public final X3ML.EntityElement entityElement;
    public final GeneratorContext generatorContext;
    public List<LabelNode> labelNodes;
    public List<AdditionalNode> additionalNodes;
    public List<Resource> resources;
    public Literal literal;
    private boolean failed;
    public static int additionalCounter=1;
    public static int namedGraphUriCounter=1;

    EntityResolver(ModelOutput modelOutput, X3ML.EntityElement entityElement, GeneratorContext generatorContext) {
        EntityResolver.modelOutput = modelOutput;
        this.entityElement = entityElement;
        this.generatorContext = generatorContext;
    }

    /*It takes as input two int numbers, representing the index of the additional node
      or the intermediate node that wants to be resolved. These indexes help us identify 
    the cases where an entity resolve is requested from the link of a mapping and therefore 
    keeping only the xapth input is not enough. We want to also keep the indexes 
    We also use the indexes of the additional or intermediate node - in cases 
    where we have "similar" nodes (with same target entity type). */
    boolean resolve(int additionalNodeIndex, int indermediateNodeIndex, boolean skip, Derivation derivedBy, String domainNamedGraph, String mappingNamedGraph) {
        if (entityElement == null) {
            throw exception("Missing entity");
        }
        if (failed) {
            return false;
        }
        if (resources == null) {
            StringBuilder unique = new StringBuilder();
            Set<String> uniqueTypes=new TreeSet<String>();
            for (TypeElement typeElement : entityElement.typeElements) {
                uniqueTypes.add(typeElement.tag);
            }
            for(String str: uniqueTypes){
                unique.append("-").append(str);
            }
            String uniqueValue="";
            /*If the type is going to be used for an additional or an intermediate node then do not re-use the old one*/
            if(additionalNodeIndex>0 || indermediateNodeIndex>0){
                if(additionalNodeIndex>0){
                    uniqueValue=unique.toString()+"-additional-"+X3ML.RootElement.linkCounter+"-"+additionalNodeIndex;
                }else{
                    uniqueValue=unique.toString()+"-intermediate-"+X3ML.RootElement.linkCounter+"-"+indermediateNodeIndex;
                }
            }
            /*If the type is going to be a Literal value (i.e. a text node), then do not re-use previous instances
            Notice that in future we should support all *literal* values*/
            if(unique.toString().contains("http://www.w3.org/2000/01/rdf-schema#Literal") || unique.toString().contains("rdfs:Literal")){
                uniqueValue="http://www.w3.org/2000/01/rdf-schema#Literal";
            }else if(unique.toString().contains("http://www.w3.org/2001/XMLSchema#dateTime") || unique.toString().contains("xsd:dateTime")){
                uniqueValue="http://www.w3.org/2001/XMLSchema#dateTime";
            }
            if(skip){
//                uniqueValue="NAMEDGRAPH_URI"+namedGraphUriCounter++;
                uniqueValue="NAMEDGRAPH_URI";
            }
                
            GeneratedValue generatedValue = entityElement.getInstance(generatorContext, uniqueValue);
            if (generatedValue == null) {
                failed = true;
                return false;
            }
            switch (generatedValue.type) {
                case URI:
                    if (resources == null) {
                        resources = new ArrayList<Resource>();
                        for (TypeElement typeElement : entityElement.typeElements) {
                            String namedGraph=null;
                            if(derivedBy==Derivation.Domain){
                                if(mappingNamedGraph!=null){
                                    namedGraph=(mappingNamedGraph.startsWith("http://") && !mappingNamedGraph.isEmpty())?mappingNamedGraph+"":"http://namedgraph/"+mappingNamedGraph;
//                                    namedGraph+=generatedValue.text.replace("http://","_").replace("uuid:", "_");
                                    X3ML.Mapping.namedGraphProduced=namedGraph;
                                }
                            }else{
                                namedGraph=X3ML.Mapping.namedGraphProduced;
                            }
                                
                            
                            resources.add(modelOutput.createTypedResource(generatedValue.text, typeElement));
                            if(domainNamedGraph!=null && !domainNamedGraph.isEmpty()){
                                X3ML.DomainElement.namedGraphProduced=(domainNamedGraph.startsWith("http://") && !domainNamedGraph.isEmpty())?domainNamedGraph+"":"http://namedgraph/"+domainNamedGraph;
//                                X3ML.DomainElement.namedGraphProduced+=generatedValue.text.replace("http://","_").replace("uuid:", "_");
                                X3ML.RootElement.hasNamedGraphs=true;
                                ModelOutput.quadGraph.add(new ResourceImpl(X3ML.DomainElement.namedGraphProduced).asNode(), 
                                        new ResourceImpl(generatedValue.text).asNode(), 
                                        new ResourceImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type").asNode(),
                                        new ResourceImpl(modelOutput.getNamespace(typeElement)).asNode());
                            }
                            if(namedGraph!=null){
                                ModelOutput.quadGraph.add(new ResourceImpl(namedGraph).asNode(),
                                        new ResourceImpl(generatedValue.text).asNode(), 
                                        new ResourceImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type").asNode(),
                                        new ResourceImpl(modelOutput.getNamespace(typeElement)).asNode());
                            }
                        }
                    }
                    labelNodes = createLabelNodes(entityElement.labelGenerators);
                    additionalNodes = createAdditionalNodes(entityElement.additionals);
                    break;
                case LITERAL:
                    literal = modelOutput.createLiteral(generatedValue.text, generatedValue.language);
                    break;
                case TYPED_LITERAL:
                    if (entityElement.typeElements.size() != 1) {
                        throw new X3MLEngine.X3MLException("Expected one type in\n" + entityElement);
                    }
                    TypeElement typeElement = entityElement.typeElements.get(0);
                    literal = modelOutput.createTypedLiteral(generatedValue.text, typeElement);
                    break;
                default:
                    throw exception("Value type " + generatedValue.type);
            }
        }
        return hasResources() || hasLiteral();
    }

    boolean hasResources() {
        return resources != null && !resources.isEmpty();
    }

    boolean hasLiteral() {
        return literal != null;
    }

    void link(Derivation derivedBy) {
        if (resources == null) {
            return;
        }
        for (Resource resource : resources) {
            if (labelNodes != null) {
                for (LabelNode labelNode : labelNodes) {
                    labelNode.linkFrom(resource, derivedBy);
                }
            }
            if (additionalNodes != null) {
                for (AdditionalNode additionalNode : additionalNodes) {
                    additionalNode.linkFrom(resource, derivedBy);
                }
            }
        }
    }

    private List<AdditionalNode> createAdditionalNodes(List<Additional> additionalList) {
        List<AdditionalNode> additionalNodes = new ArrayList<AdditionalNode>();
        if (additionalList != null) {
           
            for (Additional additional : additionalList) {
                AdditionalNode additionalNode = new AdditionalNode(modelOutput, additional, generatorContext, additionalCounter++);
                if (additionalNode.resolve()) {
                    additionalNodes.add(additionalNode);
                }
            }
        }
        return additionalNodes;
    }

    private static class AdditionalNode {

        public final ModelOutput modelOutput;
        public final Additional additional;
        public final GeneratorContext generatorContext;
        public Property property;
        public EntityResolver additionalEntityResolver;
        public final int additionalIndex;

        private AdditionalNode(ModelOutput modelOutput, Additional additional, GeneratorContext generatorContext, int additionalIndex) {
            this.modelOutput = modelOutput;
            this.additional = additional;
            this.generatorContext = generatorContext;
            this.additionalIndex=additionalIndex;
        }

        public boolean resolve() {
            property = modelOutput.createProperty(additional.relationship);
            additionalEntityResolver = new EntityResolver(modelOutput, additional.entityElement, generatorContext);
            return property != null && additionalEntityResolver.resolve(this.additionalIndex,0, false,Derivation.Additional,"","");
        }

        public void linkFrom(Resource fromResource, Derivation derivedBy) {
            additionalEntityResolver.link(Derivation.Additional);
            if (additionalEntityResolver.hasResources()) {
                for (Resource resource : additionalEntityResolver.resources) {
                    fromResource.addProperty(property, resource);
                }
            } else if (additionalEntityResolver.hasLiteral()) {
                fromResource.addLiteral(property, additionalEntityResolver.literal);
            } else {
                throw exception("Cannot link without property or literal");
            }
        }
    }

    private List<LabelNode> createLabelNodes(List<LabelGeneratorElement> generatorList) {
        List<LabelNode> newLabelNodes = new ArrayList<LabelNode>();
        if (generatorList != null) {
            for (GeneratorElement generator : generatorList) {
                LabelNode labelNode = new LabelNode(generator);
                try{
                    if (labelNode.resolve()) {
                        newLabelNodes.add(labelNode);
                    }
                    }catch(X3MLEngine.X3MLException ex){
                        X3MLEngine.exceptionMessagesList+=ex.toString();
                        Utils.printErrorMessages("ERROR FOUND: "+Utils.produceLabelGeneratorEmptyArgumentError(generator));
                    }
            }
        }
        return newLabelNodes;
    }

    private class LabelNode {

        public final GeneratorElement generator;
        public Property property;
        public Literal literal;

        private LabelNode(GeneratorElement generator) {
            this.generator = generator;
        }

        public boolean resolve() {
            if(generator.getName().equals("prefLabel")){
                property = modelOutput.createProperty(new TypeElement("skos:prefLabel", "http://www.w3.org/2004/02/skos/core#"));
            }else{
                property = modelOutput.createProperty(new TypeElement("rdfs:label", "http://www.w3.org/2000/01/rdf-schema#"));
            }
            GeneratedValue generatedValue = generatorContext.getInstance(generator, null, null,null,"-" + generator.getName());
            if (generatedValue == null) {
                return false;
            }
            switch (generatedValue.type) {
                case URI:
                    throw exception("Label node must produce a literal");
                case LITERAL:
                case TYPED_LITERAL:
                    literal = modelOutput.createLiteral(generatedValue.text, generatedValue.language);
                    return true;
            }
            return false;
        }

        public void linkFrom(Resource fromResource, Derivation derivedBy) {
            fromResource.addLiteral(property, literal);
            if(X3ML.Mapping.namedGraphProduced!=null && !X3ML.Mapping.namedGraphProduced.isEmpty()){
                ModelOutput.quadGraph.add(new ResourceImpl(X3ML.Mapping.namedGraphProduced).asNode(),
                                        fromResource.asNode(), 
                                        property.asNode(),
                                        literal.asNode());
            }if(X3ML.DomainElement.namedGraphProduced!=null && !X3ML.DomainElement.namedGraphProduced.isEmpty() && derivedBy==Derivation.Domain){
                ModelOutput.quadGraph.add(new ResourceImpl(X3ML.DomainElement.namedGraphProduced).asNode(),
                                        fromResource.asNode(), 
                                        property.asNode(),
                                        literal.asNode());
            }if(derivedBy==Derivation.Path || derivedBy==Derivation.Range){
                if(X3ML.LinkElement.namedGraphProduced!=null && !X3ML.LinkElement.namedGraphProduced.isEmpty()){
                    ModelOutput.quadGraph.add(new ResourceImpl(X3ML.LinkElement.namedGraphProduced).asNode(),
                                        fromResource.asNode(), 
                                        property.asNode(),
                                        literal.asNode());
                }
            }
        }
    }

}

enum Derivation{
    Domain,
    Path, 
    Range, 
    Additional
}
