import foo.FooBar
import metridoc.core.MetridocScript
import metridoc.tool.gorm.GormTool
/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
use(MetridocScript) {
    def gorm = includeTool(embeddedDataSource: true, GormTool)
    gorm.enableGormFor(FooBar)
    FooBar.list()
}



