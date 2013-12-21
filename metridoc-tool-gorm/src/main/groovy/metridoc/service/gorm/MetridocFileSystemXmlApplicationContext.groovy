package metridoc.service.gorm

import metridoc.utils.ClassUtils
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.springframework.beans.BeansException
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.xml.ResourceEntityResolver
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.context.support.FileSystemXmlApplicationContext

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/20/13
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
class MetridocFileSystemXmlApplicationContext extends FileSystemXmlApplicationContext{

    MetridocFileSystemXmlApplicationContext(String configLocation) {
        super(configLocation)
    }

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        beanDefinitionReader.setBeanClassLoader(ClassUtils.getDefaultClassLoader())

        // Configure the bean definition reader with this context's
        // resource loading environment.
        beanDefinitionReader.setEnvironment(this.getEnvironment());
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        // Allow a subclass to provide custom initialization of the reader,
        // then proceed with actually loading the bean definitions.
        initBeanDefinitionReader(beanDefinitionReader);
        loadBeanDefinitions(beanDefinitionReader);
    }
}
