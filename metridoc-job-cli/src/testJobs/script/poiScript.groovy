import groovy.stream.Stream

def file = new File("src/testJobs/script/locations.xlsx")
if(!file.exists()) {
    file = new File("locations.xlsx")
}

Stream.fromXlsx(file).each {
    println it
}