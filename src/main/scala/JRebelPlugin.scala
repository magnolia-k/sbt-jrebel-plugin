package fi.gekkio.sbtplugins.jrebel

import sbt._
import sbt.Keys._
import sbt.Scope.GlobalScope
import scala.xml._

object JRebelPlugin extends AutoPlugin {
  object jrebel {
    val classpath = SettingKey[Seq[File]]("jrebel-classpath")
    val enabled = SettingKey[Boolean]("jrebel-enabled")
    val rebelXml = SettingKey[File]("jrebel-rebel-xml")
    val webLinks = SettingKey[Seq[File]]("jrebel-web-links")
  }

  val jrebelGenerate = TaskKey[Seq[File]]("jrebel-generate")

  val jrebelSettings: Seq[Def.Setting[_]] = Seq[Setting[_]](
    jrebel.classpath := (Seq(Keys.classDirectory in Compile, Keys.classDirectory in Test).join).value,
    jrebel.enabled := (java.lang.Package.getPackage("com.zeroturnaround.javarebel") != null),
    jrebel.rebelXml := ((resourceManaged in Compile) { _ / "rebel.xml" }).value,
    jrebel.webLinks := Seq(),
    jrebelGenerate := rebelXmlTask.value,
    resourceGenerators in Compile += Def.task { jrebelGenerate.value }
  )

  override lazy val projectSettings = jrebelSettings

  private def dirXml(dir: File) = <dir name={ dir.absolutePath } />

  private def webLinkXml(link: File) =
    <web>
      <link>
      { dirXml(link) }
      </link>
    </web>

  private def rebelXmlTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    val enabled = jrebel.enabled.value
    val classpath = jrebel.classpath.value
    val rebelXml = jrebel.rebelXml.value
    val webLinks = jrebel.webLinks.value
    val s = state.value

    if (!enabled) Nil
    else {
      val xml =
        <application xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.zeroturnaround.com" xsi:schemaLocation="http://www.zeroturnaround.com/alderaan/rebel-2_0.xsd">
          <classpath>
           { classpath.map(dirXml) }
          </classpath>
          {
            webLinks.map(webLinkXml)
          }
        </application>

      IO.touch(rebelXml)
      XML.save(rebelXml.absolutePath, xml, "UTF-8", true)

      s.log.info("Wrote rebel.xml to %s".format(rebelXml.absolutePath))

      rebelXml :: Nil
    }
  }
}
