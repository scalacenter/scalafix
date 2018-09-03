/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require("react");

const CompLibrary = require("../../core/CompLibrary.js");

const highlightBlock = require("highlight.js");

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
              Get started
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
    const cls = this.props.className || "";
    return (
      <div className={`terminal_window terminal_window_full ${cls}`}>
        <header>
          <div className="red_btn terminal_button" />
          <div className="green_btn terminal_button" />
          <div className="yellow_btn terminal_button" />
        </header>
        <div className="terminal_window">
          <div className="terminal_text">{this.props.children}</div>
        </div>
      </div>
    );
  }
}

class Highlight extends React.Component {
  render() {
    const content = {
      __html: highlightBlock.highlight(this.props.lang, this.props.children)
        .value
    };
    return (
      <pre className={`${this.props.lang} hljs`}>
        <code dangerouslySetInnerHTML={content} />
      </pre>
    );
  }
}

// Mimic travis-ci status bar on a GitHub PR
class Travis extends React.Component {
  render() {
    return (
      <div className="github-travis merge-status-item ">
        <div className="merge-status-icon">
          <svg
            viewBox="0 0 12 16"
            version="1.1"
            width="12"
            height="16"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M7.48 8l3.75 3.75-1.48 1.48L6 9.48l-3.75 3.75-1.48-1.48L4.52 8 .77 4.25l1.48-1.48L6 6.52l3.75-3.75 1.48 1.48L7.48 8z"
            />
          </svg>
        </div>
        <img className="avatar" src="img/travis.png" alt="" />
        <div className="status-text">
          <strong className="text-emphasized">continuous-integration</strong> —
          The CI build failed
        </div>
      </div>
    );
  }
}

class Split extends React.Component {
  render() {
    return <div className="split">{this.props.children}</div>;
  }
}

class Index extends React.Component {
  render() {
    let language = this.props.language || "";
    let refactoring = `--- ForUpdateTest.scala (before scalafix)
+++ ForUpdateTest.scala (after scalafix)
-import scala.concurrent.{Await, ExecutionContext, Future}
+import scala.concurrent.Await
`;

    let linting = `Remoting.scala:15:13: error: finalize should not be used
  def finalize(): Unit = {
      ^^^^^^^^
`;

    let ci = `error: you forgot to run scalafix!
--- PostgresDriver.scala (on disk)
+++ PostgresDriver.scala (after scalafix)
-  def unloadDrivers {
+  def unloadDrivers: Unit = {
    DriverManager.getDrivers.foreach { d =>
`;

    return (
      <div className="container">
        <HomeSplash language={language} />

        <div className="feature-row">
          <Terminal>
            <Highlight lang="diff">{refactoring}</Highlight>
          </Terminal>

          <div className="split-right">
            <h3>Refactoring</h3>
            <p>
              Focus on your application and let Scalafix do grunt work like
              removing unused imports.
            </p>
          </div>
        </div>

        <div className="feature-row">
          <Terminal className="terminal-right">
            <Highlight lang="sh">{linting}</Highlight>
          </Terminal>

          <div>
            <h3>Linting</h3>
            <p>
              Some problems don't have obvious solutions. Let Scalafix report
              potential issues in your code so you catch bugs before they hit
              production.
            </p>
          </div>

        </div>

        <div className="feature-row">
          <Terminal>
            <Highlight lang="diff">{ci}</Highlight>
            <Travis />
          </Terminal>

          <div className="split-right">
            <h3>Enforce in CI</h3>
            <p>
              Ensure sure your coding style is automatically enforced by running
              Scalafix on every pull request.
            </p>
          </div>
        </div>
      </div>
    );
  }
}

module.exports = Index;
