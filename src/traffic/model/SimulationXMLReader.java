/*******************************************************************************
 * Copyright (C) 2011 - 2012 Jochen Wuttke, Jonathan Ramaswamy
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Contributors:
 *    Jochen Wuttke (wuttkej@gmail.com) - initial API and implementation
 ********************************************************************************
 *
 * Created: Dec 6, 2011
 */

package traffic.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import traffic.graph.Graph;
import traffic.graph.GraphNode;


/**
 * Reads in an XML configuration file, validates it agains the Adasim schema
 * and constructs a simulation.
 * 
 * @author Jochen Wuttke - wuttkej@gmail.com
 *
 */
final public class SimulationXMLReader {

	private static final Logger logger = Logger.getLogger(SimulationXMLReader.class);

	private Document doc;
	private static SimulationXMLBuilder builder;

	private SimulationXMLReader( File f ) throws FileNotFoundException, ConfigurationException {
		builder = new SimulationXMLBuilder();
		SAXBuilder sbuilder = new SAXBuilder(true);
		sbuilder.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
		sbuilder.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", "/Users/Jonathan/workspace/TrafficSim/resources/xml/adasim.xsd");
		sbuilder.setErrorHandler(new SimpleErrorHandler());

		try {
			doc = sbuilder.build(f);
		} catch ( FileNotFoundException e) {
			throw e;
		} catch (JDOMException e) {
			throw new ConfigurationException(e);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
	}

	/**
	 * The main interface to {@link SimulationXMLReader}. It will load all necessary files
	 * and either return a complete and valid {@link TrafficSimulator}, or it will throw
	 * and exception detailing the cause of failure.
	 * 
	 * @param config
	 * @return
	 * @throws FileNotFoundException
	 * @throws ConfigurationException
	 */
	public static TrafficSimulator buildSimulator( File config ) throws FileNotFoundException, ConfigurationException {
		try {
			SimulationXMLReader factory = new SimulationXMLReader(config);
			Graph g = builder.buildGraph( factory.doc.getRootElement().getChild("graph" ) );
			TrafficSimulator sim = new TrafficSimulator( g, factory.buildCars( factory.doc.getRootElement().getChild("cars" ), g ) );
			return sim;
		} catch ( ConfigurationException e ) {
			buildError(e);
			throw e;
		}
	}

	/**
	 * @param e
	 */
	private static void buildError(Exception e) {
		logger.error( "Problem parsing configuration file: " + e.getMessage() );
	}

	/**
	 * @return
	 * @throws ConfigurationException 
	 */
	private List<Car> buildCars( Element carsNode, Graph g ) throws ConfigurationException {
		List<Car> cars = builder.buildCars(carsNode);
		List<Car> l = new ArrayList<Car>();
		@SuppressWarnings("unchecked")
		List<Element> carNodes = carsNode.getChildren( "car" );
		for ( Element car : carNodes ) {
			Car c = validateCar(car, cars, g );
			if ( c != null ) {
				l.add(c);
			}
		}
		return l;	
	}

	/**
	 * @param car
	 * @param g
	 */
	private Car validateCar(Element car, List<Car> cars, Graph g) {
		int start = Integer.parseInt(car.getAttributeValue("start"));
		int end = Integer.parseInt(car.getAttributeValue("end"));
		int id = Integer.parseInt(car.getAttributeValue("id"));

		List<GraphNode> nodes = g.getNodes();
		try {
			Car c = getCar( id, cars );
			GraphNode node = checkEndPoint(nodes, start, id, "Start" );
			c.setStartNode(node);
			node = checkEndPoint(nodes, end, id, "End" );
			c.setEndNode(node);
			c.getStrategy().setGraph(g);
			return c;
		} catch ( ConfigurationException e ) {
			return null;
		}
	}

	/**
	 * @param id
	 * @param cars
	 * @return
	 */
	private Car getCar(int id, List<Car> cars) {
		for ( Car c : cars ) {
			if ( c.getID() == id ) return c;
		}
		return null;
	}

	/**
	 * @param nodes
	 * @param end
	 * @param id
	 * @throws ConfigurationException 
	 */
	private GraphNode checkEndPoint(List<GraphNode> nodes, int end, int id, String s) throws ConfigurationException {
		GraphNode n = isValidNode( end, nodes );
		if ( n == null ) { 
			logger.warn( s + " node " + end + " for car " + id + " does not exist");
			throw new ConfigurationException("");
		}
		return n;
	}

	/**
	 * @param end
	 */
	private GraphNode isValidNode(int id, List<GraphNode> nodes ) {
		for ( GraphNode node : nodes ) {
			if ( node.getID() == id ) return node;
		}
		return null;
	}

}
