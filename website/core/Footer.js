const React = require("react");

const siteConfig = require(process.cwd() + "/siteConfig.js");

class Footer extends React.Component {
  render() {
    const currentYear = new Date().getFullYear();
    const {
      copyright,
      colors: { secondaryColor }
    } = this.props.config;
    return (
      <footer
        className="nav-footer"
        id="footer"
        style={{ backgroundColor: secondaryColor }}
      >
        <section className="sitemap">
          {this.props.config.footerIcon && (
            <a href={this.props.config.baseUrl} className="nav-home">
              <img
                src={`${this.props.config.baseUrl}${
                  this.props.config.footerIcon
                }`}
                alt={this.props.config.title}
                width="66"
                height="58"
              />
            </a>
          )}
          <div>
            <h5>Docs</h5>
            <a
              href={`
                ${this.props.config.baseUrl}docs/users/installation.html`}
            >
              Getting Started
            </a>
            <a
              href={`
                ${this.props.config.baseUrl}docs/rules/overview.html`}
            >
              Rules
            </a>
            <a
              href={`
                ${this.props.config.baseUrl}docs/rule-authors/setup.html`}
            >
              Extend Scalafix
            </a>
          </div>
          <div>
            <h5>Community</h5>
            <a href="https://gitter.im/scalacenter/scalafix" target="_blank">
              Chat on Gitter
            </a>
            <a href="https://users.scala-lang.org/" target="_blank">
              Discuss on Scala Users
            </a>
          </div>
          <div>
            <h5>More</h5>
            <a href={siteConfig.repoUrl} target="_blank">
              GitHub
            </a>
          </div>
        </section>
        <section className="copyright">{copyright}</section>
      </footer>
    );
  }
}

module.exports = Footer;
