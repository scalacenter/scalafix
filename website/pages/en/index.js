/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require("react");

const CompLibrary = require("../../core/CompLibrary.js");

const highlightBlock = require('highlight.js');


const MarkdownBlock = CompLibrary.MarkdownBlock; /* Used to read markdown */
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

const siteConfig = require(process.cwd() + "/siteConfig.js");

function imgUrl(img) {
  return siteConfig.baseUrl + "img/" + img;
}

function docUrl(doc, language) {
  return siteConfig.baseUrl + "docs/" + (language ? language + "/" : "") + doc;
}

function pageUrl(page, language) {
  return siteConfig.baseUrl + (language ? language + "/" : "") + page;
}

class Button extends React.Component {
  render() {
    return (
      <div className="pluginWrapper buttonWrapper">
        <a className="button" href={this.props.href} target={this.props.target}>
          {this.props.children}
        </a>
      </div>
    );
  }
}

Button.defaultProps = {
  target: "_self"
};

const SplashContainer = props => (
  <div className="homeContainer">
    <div className="homeSplashFade">
      <div className="wrapper homeWrapper">{props.children}</div>
    </div>
  </div>
);

const Logo = props => (
  <div className="projectLogo">
    <img src={props.img_src} />
  </div>
);

const ProjectTitle = props => (
  <h2 className="projectTitle">
    {siteConfig.title}
    <small>{siteConfig.tagline}</small>
  </h2>
);

const PromoSection = props => (
  <div className="section promoSection">
    <div className="promoRow">
      <div className="pluginRowBlock">{props.children}</div>
    </div>
  </div>
);

class HomeSplash extends React.Component {

  render() {
    let language = this.props.language || "";
    return (
      <SplashContainer>
        <div className="inner">
          <ProjectTitle />
          <PromoSection>
            <Button href={docUrl("users/installation.html", language)}>
              Documentation
            </Button>
            <Button href={siteConfig.repoUrl} target="_blank">
              View on GitHub
            </Button>
          </PromoSection>
        </div>
      </SplashContainer>
    );
  }
}

const Block = props => (
  <Container
    padding={["bottom", "top"]}
    id={props.id}
    background={props.background}
  >
    <GridBlock align="left" contents={props.children} layout={props.layout} />
  </Container>
);

class Terminal extends React.Component {
  render() {
    const cls = this.props.className || '';
    return (
      <div className={`terminal_window terminal_window_full ${cls}`}>
        <header>
          <div className="red_btn terminal_button"></div>
          <div className="green_btn terminal_button"></div>
          <div className="yellow_btn terminal_button"></div>
        </header>
        <div className="terminal_window">
          <div className="terminal_text">
            {this.props.children}
          </div>
        </div>
      </div>
    )
  }
}

class Highlight extends React.Component {
  render () {
    const content = {
      __html: highlightBlock.highlight(this.props.lang, this.props.children).value
    }
    return (
      <pre className="{this.props.lang} hljs">
        <code dangerouslySetInnerHTML={content}></code>
      </pre>
    )
  }
}

// Mimic travis-ci status bar on a GitHub PR
class Travis extends React.Component {
  render() {
    return (
      <div className="github-travis merge-status-item ">
        <div className="merge-status-icon">
          <svg viewBox="0 0 12 16" version="1.1" width="12" height="16" aria-hidden="true"><path fillRule="evenodd" d="M12 5l-8 8-4-4 1.5-1.5L4 10l6.5-6.5L12 5z"></path></svg>
        </div>
        <img className="avatar" src="img/travis.png" alt="" />
        <div className="status-text">
          <strong className="text-emphasized">continuous-integration</strong> — The CI build passed
        </div>
      </div>
    );
  }
}

class Split extends React.Component {
  render() {
    return (
      <div className="split">
        {this.props.children}
      </div>
    )
  }
}

class Index extends React.Component {

  render() {
    let language = this.props.language || "";
    let diff =
`--- src/main/scala/com/typesafe/slick/testkit/ForUpdateTest.scala
+++ src/main/scala/com/typesafe/slick/testkit/ForUpdateTest.scala
@@ -5,11 +5,9 @@
import slick.dbio.DBIOAction
-import slick.jdbc.{SQLServerProfile, TransactionIsolation}
-import scala.concurrent.{Await, ExecutionContext, Future}
+import scala.concurrent.Await
import scala.util.Failure
`
    let ciOutput =
`[coreJVM] Running RemoveUnusedTerms+RemoveUnusedImports
[coreJVM] Running RemoveUnusedTerms+Remove… (11.76 %, 10 / …
Completed [unit] Running RemoveUnusedTerms+RemoveUnusedImports
`
    return (
      <div className="container">
        <HomeSplash language={language} />

        <div className="split">
          <Terminal>
            <Highlight lang="diff">{diff}</Highlight>
          </Terminal>

          <div className="split-right">
            <h3>Automatic Refactoring</h3>
            <p>
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed ipsum arcu, vulputate non mi non, hendrerit molestie lorem. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum erat nibh, placerat sit amet quam ac, pulvinar molestie lectus. Phasellus vitae euismod velit. In sed enim ac tortor iaculis fringilla. In quis ipsum sed elit ultrices vulputate vitae non turpis. Sed auctor mi lorem, in lobortis lorem tincidunt ut. Nulla tempor lacus et leo feugiat, eget laoreet mauris lobortis.
            </p>
          </div>
        </div>

        <div className="split">
          <div className="split-right">
            <h3>Runs in CI</h3>
            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed ipsum arcu, vulputate non mi non, hendrerit molestie lorem. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum erat nibh, placerat sit amet quam ac, pulvinar molestie lectus. Phasellus vitae euismod velit. In sed enim ac tortor iaculis fringilla. In quis ipsum sed elit ultrices vulputate vitae non turpis. Sed auctor mi lorem, in lobortis lorem tincidunt ut. Nulla tempor lacus et leo feugiat, eget laoreet mauris lobortis.</p>
          </div>

          <Terminal className="terminal-right">
            <pre className="ci-output">{ciOutput}</pre>
            <Travis/>
          </Terminal>

        </div>

        <div className="split">
          <Terminal>
            Yolo
          </Terminal>

          <div className="split-right">
            <h3>Title</h3>
            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed ipsum arcu, vulputate non mi non, hendrerit molestie lorem. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum erat nibh, placerat sit amet quam ac, pulvinar molestie lectus. Phasellus vitae euismod velit. In sed enim ac tortor iaculis fringilla. In quis ipsum sed elit ultrices vulputate vitae non turpis. Sed auctor mi lorem, in lobortis lorem tincidunt ut. Nulla tempor lacus et leo feugiat, eget laoreet mauris lobortis.</p>
          </div>


        </div>

      </div>
    );
  }

}

module.exports = Index;
