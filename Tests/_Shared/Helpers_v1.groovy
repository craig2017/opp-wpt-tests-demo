/**
 * Returns a location map for quick access to all locations for parsing
 * @return
 */
def getLocationsByRegion() {
    // create your own easy reference map here
    return [
            "cc-us-east" : [wptLocationNames: ["MA_W7_Chrome-FFX", "MA_W7_01_Chrome-FFX-IE11", "MA_W7_02_Chrome-FFX-IE10"]],
            "cc-us-west" : [wptLocationNames: []],
            "aws-us-east": [wptLocationNames: ["EC2_US_East_Ws12_Chrome-FFX-IE11"]],
            "aws-us-west": [wptLocationNames: ["EC2_US_West_Ws12_Chrome-FFX-IE11"]],
            "aws-eu-de"  : [wptLocationNames: ["EC2_EU_Frankfurt_Ws12_Chrome-FFX-IE11"]],
            "aws-ap-india"  : [wptLocationNames: ["EC2_INDIA_West_Ws12_Chrome-FFX-IE11"]]
    ]
}

/**
 * Scripts need to be xml safe for jenkins
 * @param str
 * @return
 */
def makeXmlSafe(str){
    return str.replace('&', '&amp;')
}

/**
 * Formats an environment in the URL
 * @param env
 * @return
 */
def envUrlFormat(env){
    return (env.toLowerCase() != "p2") ? "." + env : "";
}

/**
 * Creates the WPT label.  This label is used in the jenkins test name as well as how data is stored in graphite
 * @param env
 * @param app
 * @param testName
 * @param location
 * @param browser
 * @param connection
 * @param description
 * @return
 */
def getLabel(env, app, testName, location, browser, connection, description){
    def label
    if(description != null &&  description.trim() != "")
        label = String.format("%s.%s.%s.%s.%s.%s.%s", env, app, testName, location, browser, connection, description)
    else {
        label = String.format("%s.%s.%s.%s.%s.%s", env, app, testName, location, browser, connection)
    }
    return label.toLowerCase().replace(" ", "-")
}

/**
 * Gets the location based on the region and browser
 * @param region
 * @param browser
 * @return
 */
def getLocation(region, browser){
    def locationsByRegion = getLocationsByRegion()
    def location
    locationNames = (locationsByRegion[region].getAt("wptLocationNames") as List)
    if(browser.toUpperCase().contains("IE")){
        for(locationName in locationNames){
            if(locationName.contains(browser.toUpperCase())){
                location = locationName
            }
        }
    } else {
        if(region == "cc-us-east"){
            location = locationNames[0] // (Bedford_W7_Chrome-FFX) grab the first location which should be all non-ie boxes.  We have lots of instances of them.  This will provide better parallelization
        } else {
            location = locationNames[new Random().nextInt(locationNames.size())] // random location
        }
    }
    return location
}
/**
 * Formats the browser and connection so the WPT api is happy.  It basically just capitalizes them to match the WPT configuration.
 * @param browser
 * @param connection
 * @return
 */
def getBrowserAndConnection(browser, connection) {
    // returns browser.  handles special condition for IE where it converts "ie10" to "IE 10"
    return ["browser": (browser.toUpperCase().contains("IE")) ? "IE " + browser.toUpperCase().split("IE")[1] : browser.capitalize(), "connection": connection.capitalize()]
}

/**
 * Gets default SLAs
 * @return
 */
def getDefaultSLAs(){
    return """
{
   "successfulFVRuns": 6,
   "median": {
      "firstView": {
         "TTFB": 500, "SpeedIndex": 3000, "visualComplete": 3000,
         "userTime.galileo.loaded": 4000, "userTime.ctct_visually_complete": 3000,
         "breakdown": {
            "js": { "requests": 10 },
            "css": { "requests": 5 }
         }
      }
   }
}"""
}
