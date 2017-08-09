// importing helper
File sourceFile = new File(wptRootPath + "/_Shared/Helpers_v1.groovy")
Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
helper = (GroovyObject) groovyClass.newInstance();

// ----- Set configs ---------
def app = "myapp"  // THIS MUST ALREADY EXIST AND BE MAPPED IN OUR SYSTEM
def testName = "load-galileo-editor"
def testDescription = "layout-2-template" // optional
def environments = ["l1", "p2", "s1"] // list of environments you want to run against ["d1", "l1", "f1", "s1", "s1load", "p2"]
def browsers = ["firefox"] // list of browsers ["firefox", "chrome", "ie10", "ie11"]
def regions = ["cc-us-east"] // list of regions ["cc-us-east", "aws-us-east","aws-us-east2", "aws-us-west", "aws-eu-de","aws-india-west","aws-sa-east-1","aws-ap-southeast-2"]
def connections = ["cable"] // list of connection types ["cable", "native", "dsl", "fios", "3G", "4G", "dial",...]
def users = ["galileo_performance": "123456"]

// ---- additional configs ----
def testRuns = 6 // number of test iterations
def saveToGraphite = false  // saves your data to graphite as well under "ux.{env}.{appName}.{pageOrTestName}.{location}.{browser}.{connectionType}.{description-optional}"
def firstViewOnly = false  // set this to true to only run the test on an uncached page.
def hipchatRoomNotification = "My Team's Room Name"  // Get notified of runs and SLA results in your hipchat room (leave blank to disable this)

// ------ set SLAS ------------
// more info on SLAS: https://wiki.roving.com/display/EngDev/UX+CD+Performance+Pipeline+and+Trending#UXCDPerformancePipelineandTrending-PerformanceSLAs
def slas = """
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


// ---- Set WPT script
def wptScript = """
logData 0
navigate https://ui{ENV}.constantcontact.com/roving/wdk/API_LoginSiteOwner.jsp?loginName={USERNAME}&loginPassword={PASSWORD}
navigate https://em-ui{ENV}.constantcontact.com/em-ui/em/page/em-ui/email#templates/campaignType/11
logData 1
setActivityTimeout 10000
clickAndWait data-template-name={TEMPLATE_NAME}
logData 0
setActivityTimeout 1000
navigate  https://ui{ENV}.constantcontact.com/rnavmap/evaluate.rnav?activepage=site.logout.eval
"""
// removing unsafe xml characters since this will be stored in a jenkins xml file
wptScript = helper.makeXmlSafe(wptScript);


// ------- TODO: change this to meet your scripts needs
// parameterize your script
def parameterizeScript(wptScript, user, pass, env, templateName){
     return wptScript.replace("{USERNAME}", user).replace("{PASSWORD}", pass).replace("{ENV}", helper.envUrlFormat(env)).replace("{TEMPLATE_NAME}", templateName)
}

// loop through maps to create tests
environments.each() { env ->
    regions.each() { region ->
        browsers.each() { browser ->
            connections.each() { connection ->
                users.each() { user, password ->
                    // user helper methods to generate label and location
                    def label = helper.getLabel(env, app, testName, region, browser, connection, testDescription)
                    def browserConnection = helper.getBrowserAndConnection(browser, connection)
                    def location = helper.getLocation(region, browser) + ":" + browserConnection["browser"] + "." + browserConnection["connection"]

                    // ------- TODO: Modify here to parameterize your script -------
                    def script = parameterizeScript(wptScript, user, password, env, testDescription)
                    // -------------------------------------------------------------

                    // store data in map to be later executed by our jenkins job builder
                    tmpHash = DEFAULT_HASH.clone()
                    tmpHash.putAll([environment: [env], location: location, label: label, script: script, slas:slas, hipchatRoomName:hipchatRoomNotification, saveToGraphite:saveToGraphite, runs:testRuns, fvonly:(firstViewOnly)? '1' : '0'])
                    wptTests.put(label, tmpHash)
                }
            }
        }
    }
}
