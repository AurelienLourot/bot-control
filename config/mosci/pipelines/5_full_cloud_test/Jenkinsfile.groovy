// bundle-agnostic test runner. Runs tests selected by params.SELECTED_TESTS

def zaza_tests_check() {
    // check if the tests.yaml in the bundle dir contains zaza config steps
    // if it does, we will configure the job with zaza and also attempt to run zaza tests
    // if it does not, we will do legacy configuration, and run selected tests
    echo "Checking ${env.HOME}/tools/bundle_repo/${params.BUNDLE_REPO_DIR}/tests/tests.yaml for zaza configuration steps"
    tests_yaml = readFile("${env.HOME}/bundle_repo/${params.BUNDLE_REPO_DIR}/tests/tests.yaml")
    if ( tests_yaml.contains('configure: []') ) {
        echo "no zaza tests found"
        return false
    } else {
        echo "found zaza tests"
        return true
    }
}

node('master') {
    stage("Prepare dynamic test selection") {
    /* If i do it this way, all tests must have ALL params.
       i.e. if a new tests is added which requires new params,
       all previous jobs must have those params added...
    */
    echo "Cloud name set to ${params.CLOUD_NAME}"
    TESTS = params.SELECTED_TESTS.split(',')
    echo "Selected tests: ${params.SELECTED_TESTS}"
    }
    for (int i = 0; i < TESTS.size(); i++ ) { 
        stage (TESTS[i]) {
            print TESTS[i]
            build job: TESTS[i], parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
                [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
        }
    }
}

node(SLAVE_NODE_NAME) {
    stage("Run zaza tests") {
        echo "Checking if there are any zaza tests, and if so, execute."
        if ( zaza_tests_check() == true ) {
            build job: "pipeline test - openstack - zaza", parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
                [$class: 'StringParameterValue', name: 'BUNDLE_REPO_DIR', value: params.BUNDLE_REPO_DIR],
                [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
        }
    }
}
